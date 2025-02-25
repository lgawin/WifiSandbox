package dev.lgawin.sandbox.wifi

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pWfdInfo
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivityViewModel(
    private val wifiP2pManager: WifiP2pManager,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger = LogcatLogger("gawluk:MainActivityViewModel"),
) : ViewModel() {

    private val wifiDevice = MutableStateFlow<WifiDirectDevice?>(null)
    val wifiDisplayEnabled = wifiDevice
//        .onEach { logger.debug("wifi device changed", "wifiDisplayEnabled") }
        .map { it?.wfdEnabled ?: false }
    val wifiDisplayName = wifiDevice
//        .onEach { logger.debug("wifi device changed", "wifiDisplayName") }
        .map { it?.name ?: "" }

    private lateinit var channel: WifiP2pManager.Channel

    fun init(context: Context, looper: Looper) {
        channel = wifiP2pManager.initialize(context, looper) {
            logger.debug("ChannelListener", "channel disconnected")
        }
    }

    fun updateDeviceInfo() = viewModelScope.launch {
        wifiP2pManager.getDeviceInfo(channel).let {
            logger.debug("deviceInfo", "$it")
            val device = WifiDirectDevice(
                name = it.deviceName,
                status = resolveStatus(it.status),
                wfdEnabled = it.wfdInfo?.isEnabled ?: false,
            )
            wifiDevice.emit(device)
            logger.debug("deviceInfo", "emitted [$device]")
        }
    }

    private fun resolveStatus(status: Int) = when (status) {
        WifiP2pDevice.UNAVAILABLE -> WifiDirectDevice.Status.Unavailable
        WifiP2pDevice.AVAILABLE -> WifiDirectDevice.Status.Available
        WifiP2pDevice.CONNECTED -> WifiDirectDevice.Status.Connected
        WifiP2pDevice.FAILED -> WifiDirectDevice.Status.Failed
        WifiP2pDevice.INVITED -> WifiDirectDevice.Status.Invited
        else -> throw IllegalArgumentException("Unknown status: $status")
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun setWifiDisplayEnabled(enabled: Boolean) {
        logger.debug("setWifiDisplayEnabled", "?: $enabled")
        val wfdInfo = WifiP2pWfdInfo().apply {
            if (enabled) {
                isEnabled = true
                deviceType = WifiP2pWfdInfo.DEVICE_TYPE_PRIMARY_SINK
                isSessionAvailable = true
            }
        }
        try {
            wifiP2pManager.setWfdInfo(
                channel,
                wfdInfo,
                wifiP2pActionListener(onSuccess = { updateDeviceInfo() }).withLogging(logger, "setWfdInfo"),
            )
        } catch (e: SecurityException) {
            logger.error("setWfdInfo", e)
        }
    }

    fun startListening() {
        wifiP2pManager.startListening(
            channel,
            wifiP2pActionListener().withLogging(logger = logger, "startListening"),
        )
    }

    fun discoverPeers() {
        wifiP2pManager.discoverPeers(
            channel,
            wifiP2pActionListener().withLogging(logger = logger, "discoverPeers"),
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as WifiSandboxApp
                val savedStateHandle = extras.createSavedStateHandle()
                return MainActivityViewModel(
                    application.getSystemService(WifiP2pManager::class.java)!!,
                    savedStateHandle,
                    logger = LogsRepositoryLogger(
                        application.logsRepository,
                        additionalLogger = LogcatLogger("gawluk:MainActivityViewModel"),
                    ),
                ) as T
            }
        }
    }
}

private suspend fun WifiP2pManager.getDeviceInfo(channel: WifiP2pManager.Channel): WifiP2pDevice =
    suspendCancellableCoroutine { continuation ->
        requestDeviceInfo(channel) {
            it?.let(continuation::resume)
        }
    }
