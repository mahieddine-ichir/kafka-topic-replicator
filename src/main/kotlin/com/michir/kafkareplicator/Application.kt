package com.michir.kafkareplicator

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration

class Application

val appConfig = Env().load()
val logger = LogHelper.resolve()

fun main() {

    val health = Health().start(appConfig.get("server.port", Int::class.java))

    // consumer properties
    val topics = appConfig.get("kafka.consumer.topics")!!.split(",").map { it.trimIndent() }
    val pollMs = appConfig.get("kafka.consumer.poll_ms")?.toLong()?:3_000

    val kafkaConsumer = appConfig.getProperties("kafka.consumer.properties")
        .apply {
            health.healths.add(object: Health.HealthFn {
                override fun health(): HealthStatus {
                    return try {
                        val adminClient = AdminClient.create(this@apply)
                        adminClient.describeTopics(topics).values()
                            .forEach { (t, u) -> u.get().let { logger.trace("$t -> ${it.name()}, ${it.partitions()}") } }
                        HealthStatus(Status.OK, "Kafka UP")
                    } catch (e: java.lang.Exception) {
                        HealthStatus(Status.DOWN, e.message ?:"Kafka connection down")
                    }
                }
            })
        }.let {
            KafkaConsumer<ByteArray, ByteArray>(it)
                .apply { this.subscribe(topics) }
                .apply { Runtime.getRuntime().addShutdownHook(Thread { this.close() }) }
        }

    val kafkaProducer = KafkaProducer<ByteArray, ByteArray>(appConfig.getProperties("kafka.producer.properties"))
        .apply { Runtime.getRuntime().addShutdownHook(Thread { this.close() }) }

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("terminating application ... ")
        kafkaProducer.close(Duration.ofSeconds(3))
    })

    startProcess(kafkaConsumer, kafkaProducer, pollMs)
}

fun startProcess(kafkaConsumer: KafkaConsumer<ByteArray, ByteArray>,
                 kafkaProducer: KafkaProducer<ByteArray, ByteArray>,
                 pollMs: Long
) {

    while (true) {
        kafkaConsumer.poll(Duration.ofMillis(pollMs))
            .asSequence()
            .map { record -> ProducerRecord(record.topic(), record.key(), record.value())}
            .forEach { kafkaProducer.send(it) }
    }
}

