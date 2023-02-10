package com.michir.kafkareplicator

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress


class Health {

    interface HealthFn {
        fun health(): HealthStatus
    }

    val healths: MutableList<HealthFn> = mutableListOf()

    fun start(nillablePort: Int?): Health {

        val port: Int = nillablePort ?: 8080

        val contextPath = appConfig.get("server.servlet.context-path") ?.let { "$it/health" } ?: "/health"
        logger.info("health exposed on port $port, context path $contextPath")

        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext(contextPath) {
            if ("GET".equals(it.requestMethod, ignoreCase = true)) {

                val statuses = healths.map { fn -> fn.health() }.asSequence()
                val partition = statuses.partition { healthStatus -> healthStatus.status == Status.OK }

                if (partition.second.isNotEmpty()) {

                    val joined = statuses
                        .joinTo(buffer = StringBuilder(), separator = ",") { health ->
                            """
                        {
                            "status": "${health.status}",
                            "details": "${health.details}"
                        }
                        """.trimIndent()
                        }

                    val body = """
                    { "status": "${Status.DOWN}" },
                    [
                        $joined
                    ]
                """.trimIndent().toByteArray()

                    it.sendResponseHeaders(500, body.size.toLong())
                    it.responseBody.write(body)

                } else {
                    val body = """
                    { "status": "${Status.OK}" }
                """.trimIndent().toByteArray()

                    it.sendResponseHeaders(200, body.size.toLong())
                    it.responseBody.write(body)
                }
            }
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            server?.stop(3_000)
        })
        server.start()
        return this
    }
}

data class HealthStatus (
    val status: Status,
    val details: String
)

enum class Status {
    OK, DOWN
}
