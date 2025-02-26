package dev.lgawin.logger

interface Logger {
    fun verbose(tag: String, message: String)
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, throwable: Throwable) = error(tag, message = "", throwable)
}
