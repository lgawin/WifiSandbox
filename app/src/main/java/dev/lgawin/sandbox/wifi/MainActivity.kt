package dev.lgawin.sandbox.wifi

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dev.lgawin.logger.LogInfoEntity
import dev.lgawin.logger.LogcatLogger
import dev.lgawin.logger.LogsRepositoryLogger
import dev.lgawin.sandbox.wifi.ui.theme.WifiSandboxTheme
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainActivityViewModel> { MainActivityViewModel.Factory }

    private val logsRepository by lazy { WifiSandboxApp.from(this).logsRepository }
    private val logger by lazy {
        LogsRepositoryLogger(
            logsRepository,
            additionalLogger = LogcatLogger("gawluk:MainActivity"),
        )
    }

    private val preferences by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }

    private var goToSettings: Boolean
        get() = preferences.getBoolean("goToSettings", false)
        set(value) = preferences.edit(commit = true) { putBoolean("goToSettings", value) }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        logger.debug("onCreate", "start")
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        lifecycleScope.launch {
            val wifiDirectEventFlow =
                WiFiDirectBroadcastReceiver(logger = logger).observeIn(this@MainActivity)
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                wifiDirectEventFlow.collect { logger.debug("event", it.toString()) }
            }
        }

        viewModel.init(context = this, looper = mainLooper)
        viewModel.updateDeviceInfo()

        setContent {
            WifiSandboxTheme {
                Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp).fillMaxSize(),
                    ) {
                        val nearbyWifiPermissionState =
                            rememberPermissionState(android.Manifest.permission.NEARBY_WIFI_DEVICES)

                        LaunchedEffect(nearbyWifiPermissionState.status.isGranted) {
                            viewModel.updateDeviceInfo()
                        }

                        Column(
                            modifier = Modifier.wrapContentSize(),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            val displayName by viewModel.wifiDisplayName.collectAsState(initial = "")
                            val isDisplayEnabled by viewModel.wifiDisplayEnabled.collectAsState(initial = false)
                            val logs by logsRepository.logs.collectAsState()

                            Greeting(name = getString(R.string.app_name))
                            Spacer(modifier = Modifier.height(30.dp))

                            CompositionLocalProvider(
                                LocalTextStyle provides MaterialTheme.typography.headlineMedium,
                            ) {
                                if (nearbyWifiPermissionState.status.isGranted) {
                                    Column {
                                        Text("Peers:")
                                        Button(onClick = { viewModel.discoverPeers() }) { Text("Discover peers") }
                                        Text("Display:")
                                        DisplayInfo(
                                            displayName,
                                            isDisplayEnabled,
                                            onRefreshClick = { viewModel.updateDeviceInfo() },
                                            onWifiEnabledChange = { viewModel.setWifiDisplayEnabled(it) },
                                            onStartListeningClick = { viewModel.startListening() },
                                        )
                                    }
                                } else {
                                    val textToShow =
                                        when {
                                            nearbyWifiPermissionState.status.shouldShowRationale ->
                                                "The NEARBY_WIFI_DEVICES is required for this app. " +
                                                    "Please grant the permission."

                                            goToSettings ->
                                                "NEARBY_WIFI_DEVICES permission required for this feature to be available. " +
                                                    "Go to settings and grant the permission"

                                            else ->
                                                "NEARBY_WIFI_DEVICES permission required for this feature to be available. " +
                                                    "Please grant the permission"
                                        }
                                    Column(modifier = Modifier.padding(all = 16.dp)) {
                                        Text(textToShow)
                                        Button(
                                            onClick = {
                                                if (goToSettings) {
                                                    goToSettings()
                                                } else {
                                                    nearbyWifiPermissionState.launchPermissionRequest()
                                                    goToSettings = nearbyWifiPermissionState.status.shouldShowRationale
                                                }
                                            },
                                        ) {
                                            val buttonText =
                                                if (goToSettings) "Go to settings" else "Request permission"
                                            Text(buttonText)
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Text("Logs (${logs.size}):")
                                    if (logs.isNotEmpty()) {
                                        Button(onClick = { logsRepository.clear() }) {
                                            Text("Clear")
                                        }
                                    }
                                }
                            }

                            LogsPane(
                                logs,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }

    fun goToSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        startActivity(intent)
    }
}

@Composable
private fun ColumnScope.DisplayInfo(
    displayName: String,
    isDisplayEnabled: Boolean,
    onRefreshClick: () -> Unit,
    onWifiEnabledChange: (Boolean) -> Unit,
    onStartListeningClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(
            onClick = onRefreshClick,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
        }
        Text(
            text = buildAnnotatedString {
                append("display name: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(displayName)
                }
            }
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Checkbox(
            checked = isDisplayEnabled,
            onCheckedChange = onWifiEnabledChange
        )
        Text("Is display enabled?")
        if (isDisplayEnabled) {
            Button(onClick = onStartListeningClick) {
                Text("Start listening")
            }
        }
    }
}

@Composable
private fun LogsPane(logs: List<LogInfoEntity>, modifier: Modifier = Modifier) {
    val collapsed = remember { mutableStateListOf<UUID>() }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs.reversed()) {
            LogsItem(
                it,
                onItemClicked = { id ->
                    if (collapsed.contains(id)) {
                        collapsed.remove(id)
                    } else {
                        collapsed.add(id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                collapsed = collapsed.contains(it.id),
            )
        }
    }
}

@Composable
fun LogsItem(
    logInfoEntity: LogInfoEntity,
    onItemClicked: (UUID) -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
) {
    val textColor = when (logInfoEntity.level) {
        "Error" -> Color.Red
        else -> LocalContentColor.current
    }
    var showExpand by remember { mutableStateOf(collapsed) }
    val minimumLineLength = 2
    val maxLines = if (collapsed) minimumLineLength else Int.MAX_VALUE

    Column(modifier = modifier.run {
        if (showExpand) {
            clickable { onItemClicked(logInfoEntity.id) }
        } else this
    }) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val currentTextStyle = LocalTextStyle.current
            Text(logInfoEntity.time)
            CompositionLocalProvider(LocalTextStyle provides currentTextStyle.copy(fontWeight = FontWeight.Bold)) {
                Text(logInfoEntity.level)
                Text(":")
                Text(logInfoEntity.tag)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                if (showExpand) {
                    Icon(
                        imageVector = if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "",
                    )
                }
            }

        }
        listOfNotNull(
            logInfoEntity.message,
            logInfoEntity.stackTrace,
        ).joinToString(", ")
            .takeUnless { it.isEmpty() }
            ?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = 8.dp),
                    color = textColor,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult: TextLayoutResult ->
                        if (textLayoutResult.lineCount > minimumLineLength) {
                            if (textLayoutResult.isLineEllipsized(minimumLineLength)) showExpand = true
                        }
                    }
                )
            }
    }
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


data class WifiDirectDevice(
    val name: String,
    val wfdEnabled: Boolean,
    val status: Status,
) {
    enum class Status {
        Unavailable,
        Available,
        Connected,
        Invited,
        Failed,
    }
}
