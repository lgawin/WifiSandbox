package dev.lgawin.sandbox.wifi

import android.app.Application
import android.content.Context

class WifiSandboxApp : Application() {

    val logsRepository: LogsRepository by lazy { DefaultLogsRepository() }

    companion object {
        fun from(context: Context) = context as? WifiSandboxApp ?: context.applicationContext as WifiSandboxApp
    }
}
