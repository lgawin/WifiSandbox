package dev.lgawin.sandbox.wifi

import android.util.Log

interface Logger {
    fun debug(tag: String, message: String)
    fun error(tag: String, throwable: Throwable)
}

@Suppress("FunctionName")
fun LogcatLogger(tag: String? = null) = object : Logger {

    private val rootTag = tag

    override fun debug(tag: String, message: String) {
        val finalMessage = if (rootTag == null) message else "$tag: $message"
        Log.d(rootTag ?: tag, finalMessage)
    }

    override fun error(tag: String, throwable: Throwable) {
        val message = "failed"
        val finalMessage = if (rootTag == null) message else "$tag: $message"
        Log.e(rootTag ?: tag, finalMessage, throwable)
    }
}