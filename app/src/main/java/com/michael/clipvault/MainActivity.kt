package com.michael.clipvault

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michael.clipvault.core.common.ServiceLocator
import com.michael.clipvault.feature.clipboard.ClipboardHistoryViewModel
import com.michael.clipvault.feature.clipboard.ClipboardMonitorService
import com.michael.clipvault.feature.clipboard.HistoryVaultScreen
import com.michael.clipvault.feature.patterns.PatternsScreen
import com.michael.clipvault.feature.patterns.PatternsViewModel
import com.michael.clipvault.feature.settings.SettingsScreen
import com.michael.clipvault.feature.transform.TransformScreen
import com.michael.clipvault.feature.transform.TransformViewModel
import com.michael.clipvault.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification alert triggers active", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize ServiceLocator DB & Keys
        ServiceLocator.initialize(applicationContext)

        // 2. Start Service if configured enabled in local preferences
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val enabled = ServiceLocator.getPreferencesManager().monitorClipboardFlow.first()
                if (enabled) {
                    val serviceIntent = Intent(applicationContext, ClipboardMonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            } catch (e: Exception) {
                // Ignore service startups inside simulated previews
            }
        }

        // 3. Request permissions on Android 13+ (POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }

    // Capture focus change to automatically check background text for modern Android
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Instantly sync latest copies when user switches back to ClipVault
            val sysClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = sysClipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: ""
                if (text.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val historyRepo = ServiceLocator.getHistoryRepository()
                        val list = historyRepo.observeHistory().first()
                        val alreadyProcessed = list.any { it.preview == text || (text.length > 97 && it.preview.startsWith(text.take(97))) }
                        if (!alreadyProcessed) {
                            // Process clip safely
                            val intent = Intent(applicationContext, ClipboardMonitorService::class.java)
                            intent.putExtra("EXTRA_MANUAL_CAPTURE", text)
                            // Starts monitor to record and prompt
                            val patternRepo = ServiceLocator.getPatternRepository()
                            val activePatterns = patternRepo.getActivePatterns()
                            var matchedAction: com.michael.clipvault.core.domain.ClipboardAction? = null
                            for (p in activePatterns) {
                                try {
                                    val r = Regex(p.regex)
                                    val m = r.find(text)
                                    if (m != null) {
                                        matchedAction = com.michael.clipvault.core.domain.ClipboardAction(
                                            patternId = p.id,
                                            label = p.label,
                                            matchedText = m.value,
                                            groups = m.groupValues,
                                            actionType = p.actionType,
                                            actionPayload = p.actionPayload,
                                            originalText = text
                                        )
                                        patternRepo.incrementMatchCount(p.id)
                                        break
                                    }
                                } catch (e: Exception) {}
                            }
                            
                            val act = matchedAction ?: com.michael.clipvault.core.domain.ClipboardAction.noMatch(text)
                            historyRepo.insert(com.michael.clipvault.core.domain.ClipboardEntry(
                                preview = text,
                                timestampMillis = System.currentTimeMillis(),
                                patternMatchId = act.patternId,
                                patternLabel = if (act.patternId != null) act.label else null,
                                matchedText = if (act.patternId != null) act.matchedText else null,
                                tags = if (act.patternId != null) listOf("Matched", act.label) else listOf("Plaintext")
                            ))
                            
                            val showNotifs = ServiceLocator.getPreferencesManager().showNotificationsFlow.first()
                            if (showNotifs && act.patternId != null) {
                                val helper = com.michael.clipvault.feature.clipboard.NotificationHelper(this@MainActivity)
                                helper.showActionNotification(act)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun MainAppLayout() {
    var selectedScreen by remember { mutableStateOf("history") }

    val historyViewModel: ClipboardHistoryViewModel = viewModel()
    val patternsViewModel: PatternsViewModel = viewModel()
    val transformViewModel: TransformViewModel = viewModel()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedScreen == "history",
                    onClick = { selectedScreen = "history" },
                    icon = {
                        Icon(
                            if (selectedScreen == "history") Icons.Filled.ContentPaste else Icons.Outlined.ContentPaste,
                            contentDescription = "History Vault"
                        )
                    },
                    label = { Text("Vault") }
                )

                NavigationBarItem(
                    selected = selectedScreen == "patterns",
                    onClick = { selectedScreen = "patterns" },
                    icon = {
                        Icon(
                            if (selectedScreen == "patterns") Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Regex Engine"
                        )
                    },
                    label = { Text("Engine") }
                )

                NavigationBarItem(
                    selected = selectedScreen == "transform",
                    onClick = { selectedScreen = "transform" },
                    icon = {
                        Icon(
                            if (selectedScreen == "transform") Icons.Filled.ChangeCircle else Icons.Outlined.ChangeCircle,
                            contentDescription = "Pipeline Hub"
                        )
                    },
                    label = { Text("Pipeline") }
                )

                NavigationBarItem(
                    selected = selectedScreen == "settings",
                    onClick = { selectedScreen = "settings" },
                    icon = {
                        Icon(
                            if (selectedScreen == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedScreen,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut())
            },
            modifier = Modifier.padding(innerPadding),
            label = "ScreenTransitions"
        ) { screen ->
            when (screen) {
                "history" -> HistoryVaultScreen(
                    viewModel = historyViewModel,
                    onActiveScanTriggered = { text ->
                        scope.launch(Dispatchers.IO) {
                            val historyRepo = ServiceLocator.getHistoryRepository()
                            val patternRepo = ServiceLocator.getPatternRepository()
                            
                            val activePatterns = patternRepo.getActivePatterns()
                            var matchedAction: com.michael.clipvault.core.domain.ClipboardAction? = null
                            for (p in activePatterns) {
                                try {
                                    val r = Regex(p.regex)
                                    val m = r.find(text)
                                    if (m != null) {
                                        matchedAction = com.michael.clipvault.core.domain.ClipboardAction(
                                            patternId = p.id,
                                            label = p.label,
                                            matchedText = m.value,
                                            groups = m.groupValues,
                                            actionType = p.actionType,
                                            actionPayload = p.actionPayload,
                                            originalText = text
                                        )
                                        patternRepo.incrementMatchCount(p.id)
                                        break
                                    }
                                } catch (e: Exception) {}
                            }
                            
                            val act = matchedAction ?: com.michael.clipvault.core.domain.ClipboardAction.noMatch(text)
                            historyRepo.insert(com.michael.clipvault.core.domain.ClipboardEntry(
                                preview = text,
                                timestampMillis = System.currentTimeMillis(),
                                patternMatchId = act.patternId,
                                patternLabel = if (act.patternId != null) act.label else null,
                                matchedText = if (act.patternId != null) act.matchedText else null,
                                tags = if (act.patternId != null) listOf("Matched", act.label) else listOf("Plaintext")
                            ))
                            
                            val showNotifs = ServiceLocator.getPreferencesManager().showNotificationsFlow.first()
                            if (showNotifs && act.patternId != null) {
                                val helper = com.michael.clipvault.feature.clipboard.NotificationHelper(context)
                                helper.showActionNotification(act)
                            }
                        }
                    }
                )
                "patterns" -> PatternsScreen(
                    viewModel = patternsViewModel
                )
                "transform" -> TransformScreen(
                    viewModel = transformViewModel
                )
                "settings" -> SettingsScreen(
                    onHistoryWiped = {
                        historyViewModel.clearAllHistory()
                    }
                )
            }
        }
    }
}
