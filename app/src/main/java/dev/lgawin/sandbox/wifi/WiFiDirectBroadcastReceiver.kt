package dev.lgawin.sandbox.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.EXTRA_DISCOVERY_STATE
import android.net.wifi.p2p.WifiP2pManager.EXTRA_NETWORK_INFO
import android.net.wifi.p2p.WifiP2pManager.EXTRA_P2P_DEVICE_LIST
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_GROUP
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_INFO
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach

class WiFiDirectBroadcastReceiver(private val logger: Logger = LogcatLogger("gawluk:receiver")) : BroadcastReceiver() {

    private val _events = MutableSharedFlow<WifiDirectEvent>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<WifiDirectEvent> = _events.asSharedFlow()

    override fun onReceive(context: Context, intent: Intent) {
        logger.debug("onReceive", intent.toString())
        when (intent.action) {
            WIFI_P2P_PEERS_CHANGED_ACTION -> {
                val deviceList = intent.getParcelableExtra(EXTRA_P2P_DEVICE_LIST, WifiP2pDeviceList::class.java)!!
                val devices = deviceList.deviceList.toList()
                logger.debug("WIFI_P2P_PEERS_CHANGED_ACTION", devices.toString())
                val emitResult = _events.tryEmit(WifiDirectEvent.DeviceListUpdate(devices))
                logger.debug("onReceive", " event sent? $emitResult")
            }

            WIFI_P2P_STATE_CHANGED_ACTION -> {
                val wifiP2pState = intent.getIntExtra(EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED)
                logger.debug("WIFI_P2P_STATE_CHANGED_ACTION", dumpWifiP2pState(wifiP2pState))
            }

            WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val p2pInfo = intent.getParcelableExtra(EXTRA_WIFI_P2P_INFO, WifiP2pInfo::class.java)
                val networkInfo = intent.getParcelableExtra(EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                val p2pGroup = intent.getParcelableExtra(EXTRA_WIFI_P2P_GROUP, WifiP2pGroup::class.java)
                logger.debug(
                    "WIFI_P2P_CONNECTION_CHANGED_ACTION",
                    "p2pInfo=$p2pInfo\nnetworkInfo=$networkInfo\np2pGroup=$p2pGroup"
                )
            }

            WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                val state = intent.getIntExtra(EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED)
                logger.debug("WIFI_P2P_DISCOVERY_CHANGED_ACTION", dumpWifiP2pDiscoveryState(state))
            }

            WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val p2pDevice = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                logger.debug("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION", p2pDevice.toString())
            }
        }
    }

    companion object {
        internal val INTENT_FILTER = IntentFilter().apply {
            addAction(WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }
}

sealed interface WifiDirectEvent {
    data class DeviceListUpdate(val devices: List<WifiP2pDevice>) : WifiDirectEvent
}

fun WiFiDirectBroadcastReceiver.observeIn(context: Context): Flow<WifiDirectEvent> = callbackFlow {
    val logger = LogcatLogger("gawluk:receiver")
    val receiver = this@observeIn
    logger.debug("observe", "register receiver")
    context.registerReceiver(receiver, WiFiDirectBroadcastReceiver.INTENT_FILTER, Context.RECEIVER_NOT_EXPORTED)
    receiver.events.onEach(::trySend)
    awaitClose {
        logger.debug("observe", "unregister receiver")
        context.unregisterReceiver(receiver)
    }
}

private fun dumpWifiP2pState(state: Int): String = when (state) {
    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> "WIFI_P2P_STATE_ENABLED"
    WifiP2pManager.WIFI_P2P_STATE_DISABLED -> "WIFI_P2P_STATE_DISABLED"
    else -> throw IllegalArgumentException("unknown state: $state")
}

private fun dumpWifiP2pDiscoveryState(state: Int): String = when (state) {
    WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> "WIFI_P2P_DISCOVERY_STARTED"
    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> "WIFI_P2P_DISCOVERY_STOPPED"
    else -> throw IllegalArgumentException("unknown state: $state")
}
