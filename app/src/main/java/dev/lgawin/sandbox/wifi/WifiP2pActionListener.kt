package dev.lgawin.sandbox.wifi

import android.net.wifi.p2p.WifiP2pManager

private class DebuggingActionListener(
    private val logger: Logger,
    private val operation: String,
    private val wrappedListener: WifiP2pManager.ActionListener? = null,
) : WifiP2pManager.ActionListener {

    override fun onSuccess() {
        logger.debug(operation, "success")
        wrappedListener?.onSuccess()
    }

    override fun onFailure(reason: Int) {
        logger.error(operation, RuntimeException(dumpError(reason)))
        wrappedListener?.onFailure(reason)
    }

    private fun dumpError(reason: Int): String =
        when (reason) {
            WifiP2pManager.ERROR -> "ERROR"
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
            WifiP2pManager.BUSY -> "BUSY"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
            else -> throw IllegalArgumentException("Unknown reason: $reason")
        }
}

interface WifiP2pActionListener : WifiP2pManager.ActionListener

fun wifiP2pActionListener(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) =
    object : WifiP2pActionListener {
        override fun onSuccess() = onSuccess()

        override fun onFailure(reason: Int) = onFailure(reason)
    }

fun WifiP2pActionListener.withLogging(logger: Logger, operationName: String): WifiP2pManager.ActionListener =
    DebuggingActionListener(
        logger = logger,
        operation = operationName,
        wrappedListener = this,
    )

fun WifiP2pActionListener.withLogging(operationName: String): WifiP2pManager.ActionListener =
    DebuggingActionListener(
        logger = LogcatLogger(tag = "DebuggingActionListener"),
        operation = operationName,
        wrappedListener = this,
    )