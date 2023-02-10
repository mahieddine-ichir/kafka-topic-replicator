package com.michir.kafkareplicator

import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger

class LogHelper {

    companion object {
        fun resolve() : Logger {
            appConfig.get("log.level")?.let { LogManager.getRootLogger().level = Level.toLevel(it.uppercase()) }
            return Logger.getLogger(Application::class.java)
        }
    }
}