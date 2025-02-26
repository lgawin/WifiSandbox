package dev.lgawin.logger

import android.util.Log

@Suppress("FunctionName")
fun LogcatLogger(tag: String? = null) = object : Logger {

    private val rootTag = tag

    private fun buildMessage(tag: String, message: String): Pair<String, String> =
        (rootTag ?: tag) to (if (rootTag == null) message else "$tag: $message")

    override fun verbose(tag: String, message: String) {
        val (logTag, logMessage) = buildMessage(tag, message)
        Log.d(logTag, logMessage)
    }

    override fun debug(tag: String, message: String) {
        val (logTag, logMessage) = buildMessage(tag, message)
        Log.d(logTag, logMessage)
    }

    override fun info(tag: String, message: String) {
        val (logTag, logMessage) = buildMessage(tag, message)
        Log.d(logTag, logMessage)
    }

    override fun warn(tag: String, message: String) {
        val (logTag, logMessage) = buildMessage(tag, message)
        Log.d(logTag, logMessage)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        val (logTag, logMessage) = buildMessage(tag, message)
        if (throwable != null) {
            Log.e(logTag, logMessage, throwable)
        } else {
            Log.e(logTag, logMessage)
        }
    }
}