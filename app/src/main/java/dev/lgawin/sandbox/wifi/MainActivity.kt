package dev.lgawin.sandbox.wifi

import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pWfdInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.getSystemService
import dev.lgawin.sandbox.wifi.ui.theme.WifiSandboxTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var wifiDisplayName = MutableStateFlow("")
        val wifiP2pManager = getSystemService<WifiP2pManager>()
        wifiP2pManager?.let { p2p ->
            val channel = p2p.initialize(this, mainLooper, object : WifiP2pManager.ChannelListener {
                override fun onChannelDisconnected() {
                    TODO("Not yet implemented")
                }
            })
            Log.d("gawluk", "channel: $channel")
            val wfdInfo = WifiP2pWfdInfo().apply {
                isEnabled = true
                deviceType = WifiP2pWfdInfo.DEVICE_TYPE_PRIMARY_SINK
                isSessionAvailable = true
                controlPort = 7236
                maxThroughput = 50
            }
            p2p.setWfdInfo(channel, wfdInfo, object : WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Log.d("gawluk", "setWfdInfo: onSuccess")
                }

                override fun onFailure(reason: Int) {
                    Log.d("gawluk", "setWfdInfo: onFailure: ${dumpError(reason)}")
                }
            })

            p2p.requestDeviceInfo(channel) {
                Log.d("gawluk", "device: $it")
                wifiDisplayName.value = it?.deviceName ?: ""
            }

//            openControlPortOnWifiDirect()

            p2p.startListening(channel, object : WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Log.d("gawluk", "startListening: onSuccess")
                }

                override fun onFailure(reason: Int) {
                    Log.d("gawluk", "startListening: onFailure: ${dumpError(reason)}")
                }
            })

            p2p.requestConnectionInfo(channel) {
                Log.d("gawluk", "connectionInfo: $it")
            }
        }

        enableEdgeToEdge()
        setContent {
            WifiSandboxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private fun dumpError(reason: Int): String = when (reason) {
    WifiP2pManager.ERROR -> "ERROR"
    WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
    WifiP2pManager.BUSY -> "BUSY"
    WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
    else -> throw IllegalArgumentException("Unknown reason: $reason")
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WifiSandboxTheme {
        Greeting("Android")
    }
}