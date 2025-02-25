package dev.lgawin.sandbox.wifi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface LogsRepository {
    val logs: StateFlow<List<LogInfoEntity>>
    fun append(logInfo: LogInfo)
}

class DefaultLogsRepository : LogsRepository {
    private val _logs = MutableStateFlow(listOf<LogInfoEntity>())
    override val logs: StateFlow<List<LogInfoEntity>> = _logs.asStateFlow()

    override fun append(logInfo: LogInfo) {
        _logs.update { it.plus(logInfo.toEntity()) }
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