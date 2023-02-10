# kafka-topic-replicator
A lightweight (no framework, no spring boot) Kafka topic replicator, written in Kotlin, based on a simple `while(true)` kafka 
poll consumer / producer.

- exposes, by default a health check on context path `/health` (exposed using Java HTTP Server)
- polls Kafka records using kafka-clients 2.7.1
- Java version 18, Kotlin 1.8.8 version
- Config properties are loaded from application(-profile).properties, can be overridden

## Configuration

- `kafka.consumer.poll_ms` consumer poll ms
- `kafka.consumer.topics` list of input topics to replicate
- `kafka.consumer.properties.*` list kafka consumer properties, as defined in official documentation
- `kafka.producer.properties` list of kafka producer properties, as defined in official documentation
