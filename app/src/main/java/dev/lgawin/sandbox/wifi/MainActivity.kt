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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            p2p.setWfdInfo(channel, wfdInfo, object : WifiP2pManager.ActionListener {
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

            p2p.startListening(channel, object : WifiP2pManager.ActionListener {
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
                Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.wrapContentSize(),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            val displayName by wifiDisplayName.collectAsState()
                            Greeting(name = getString(R.string.app_name))
                            Spacer(modifier = Modifier.height(30.dp))
                            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineMedium) {
                                Text(text = "Wifi p2p supported? ${wifiP2pManager != null}")
                                Text(
                                    text = buildAnnotatedString {
                                        append("display name: ")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(displayName)
                                        }
                                    }
                                )
                            }
                        }
                    }
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
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WifiSandboxTheme {
        Greeting("Android")
    }
}