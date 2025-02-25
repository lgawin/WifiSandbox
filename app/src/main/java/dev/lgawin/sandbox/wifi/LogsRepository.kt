package dev.lgawin.sandbox.wifi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.*

interface LogsRepository {
    val logs: StateFlow<List<LogInfoEntity>>
    fun append(logInfo: LogInfo)
    fun clear()
}

class DefaultLogsRepository : LogsRepository {
    private val _logs = MutableStateFlow(listOf<LogInfoEntity>())
    override val logs: StateFlow<List<LogInfoEntity>> = _logs.asStateFlow()

    override fun append(logInfo: LogInfo) {
        _logs.update { it.plus(logInfo.toEntity()) }
    }

    override fun clear() {
        _logs.value = emptyList()
    }
}

private fun LogInfo.toEntity() = LogInfoEntity(
    id = this.id,
    time = this.timeStamp.toString(),
    level = this.level.name,
    tag = this.tag,
    message = this.message,
    stackTrace = this.throwable?.stackTraceToString(),
)

data class LogInfo(
    val level: LogLevel,
    val tag: String,
    val message: String? = null,
    val throwable: Throwable? = null,
) {
    val timeStamp: Instant = Instant.now()
    val id = UUID.randomUUID()

    enum class LogLevel {
        Debug,
        Error,
    }
}

data class LogInfoEntity(
    val id: UUID,
    val time: String,
    val level: String,
    val tag: String,
    val message: String?,
    val stackTrace: String? = null,
)

@Suppress("FunctionName")
fun LogsRepositoryLogger(logsRepository: LogsRepository, additionalLogger: Logger? = null) = object : Logger {
    override fun debug(tag: String, message: String) {
        logsRepository.append(LogInfo(LogInfo.LogLevel.Debug, tag, message = message))
        additionalLogger?.debug(tag, message)
    }

    override fun error(tag: String, throwable: Throwable) {
        logsRepository.append(LogInfo(LogInfo.LogLevel.Error, tag, throwable = throwable))
        additionalLogger?.error(tag, throwable)
    }
}
