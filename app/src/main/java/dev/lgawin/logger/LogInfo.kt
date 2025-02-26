package dev.lgawin.logger

import java.time.Instant
import java.util.*

data class LogInfo(
    val level: LogLevel,
    val tag: String,
    val message: String? = null,
    val throwable: Throwable? = null,
) {
    val timeStamp: Instant = Instant.now()
    val id = UUID.randomUUID()

    enum class LogLevel {
        Verbose,
        Info,
        Debug,
        Warn,
        Error,
    }
}