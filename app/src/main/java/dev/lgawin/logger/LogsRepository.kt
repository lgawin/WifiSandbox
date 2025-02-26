package dev.lgawin.logger

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*

interface LogsRepository {
    val logs: StateFlow<List<LogInfoEntity>>
    fun append(log: LogInfoEntity)
    fun clear()
}

class DefaultLogsRepository : LogsRepository {
    private val _logs = MutableStateFlow(listOf<LogInfoEntity>())
    override val logs: StateFlow<List<LogInfoEntity>> = _logs.asStateFlow()

    override fun append(log: LogInfoEntity) {
        _logs.update { it.plus(log) }
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

    override fun verbose(tag: String, message: String) {
        val logInfoEntity = LogInfo(LogInfo.LogLevel.Verbose, tag, message = message).toEntity()
        logsRepository.append(logInfoEntity)
        additionalLogger?.debug(tag, message)
    }

    override fun debug(tag: String, message: String) {
        val logInfoEntity = LogInfo(LogInfo.LogLevel.Debug, tag, message = message).toEntity()
        logsRepository.append(logInfoEntity)
        additionalLogger?.debug(tag, message)
    }

    override fun info(tag: String, message: String) {
        val logInfoEntity = LogInfo(LogInfo.LogLevel.Info, tag, message = message).toEntity()
        logsRepository.append(logInfoEntity)
        additionalLogger?.debug(tag, message)
    }

    override fun warn(tag: String, message: String) {
        val logInfoEntity = LogInfo(LogInfo.LogLevel.Warn, tag, message = message).toEntity()
        logsRepository.append(logInfoEntity)
        additionalLogger?.debug(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        val logInfoEntity = LogInfo(LogInfo.LogLevel.Error, tag, message = message, throwable = throwable).toEntity()
        logsRepository.append(logInfoEntity)
        additionalLogger?.error(tag, message, throwable)
    }
}
