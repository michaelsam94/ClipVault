package com.michael.clipvault.feature.clipboard

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.michael.clipvault.core.common.ServiceLocator
import com.michael.clipvault.core.domain.ClipboardAction
import com.michael.clipvault.core.domain.ClipboardEntry
import com.michael.clipvault.core.domain.RegexPattern
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ClipboardMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val processingScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var notificationHelper: NotificationHelper
    private var clipboardManager: ClipboardManager? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
        notificationHelper = NotificationHelper(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Start Foreground Service using the mandatory Material-silent channel notification
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            notificationHelper.buildServiceNotification()
        )

        observeClipboardState()
    }

    private fun observeClipboardState() {
        serviceScope.launch {
            ServiceLocator.getPreferencesManager().monitorClipboardFlow.collect { enabled ->
                if (enabled) {
                    try {
                        clipboardManager?.removePrimaryClipChangedListener(clipListener)
                        clipboardManager?.addPrimaryClipChangedListener(clipListener)
                    } catch (e: Exception) {
                        Log.e("ClipboardService", "Error setting clip listener", e)
                    }
                } else {
                    clipboardManager?.removePrimaryClipChangedListener(clipListener)
                }
            }
        }
    }

    private fun handleClipboardChange() {
        val clipData = clipboardManager?.primaryClip ?: return
        if (clipData.itemCount > 0) {
            val textItem = clipData.getItemAt(0).coerceToText(this)
            val clipText = textItem?.toString() ?: return
            
            if (clipText.isEmpty()) return

            // Immediately dispatch CPU-bound pattern matching tasks off the Main Thread
            processingScope.launch {
                processClipboardText(clipText)
            }
        }
    }

    suspend fun processClipboardText(rawText: String): ClipboardAction = withContext(Dispatchers.Default) {
        val patternRepo = ServiceLocator.getPatternRepository()
        val historyRepo = ServiceLocator.getHistoryRepository()

        // 1. Fetch active patterns
        val activePatterns = patternRepo.getActivePatterns()

        // 2. Perform local matching
        var matchedAction: ClipboardAction? = null
        for (pattern in activePatterns) {
            try {
                val regex = Regex(pattern.regex)
                val matchResult = regex.find(rawText)
                if (matchResult != null) {
                    // Collect matched text and regex groups values
                    val groups = matchResult.groupValues
                    matchedAction = ClipboardAction(
                        patternId = pattern.id,
                        label = pattern.label,
                        matchedText = matchResult.value,
                        groups = groups,
                        actionType = pattern.actionType,
                        actionPayload = pattern.actionPayload,
                        originalText = rawText
                    )
                    
                    // Increment match count locally in SQLite Database
                    patternRepo.incrementMatchCount(pattern.id)
                    break
                }
            } catch (e: Exception) {
                Log.e("ClipboardService", "Invalid regex pattern error in compile: ${pattern.regex}", e)
            }
        }

        val action = matchedAction ?: ClipboardAction.noMatch(rawText)

        // 3. Save into encrypted database history
        val entry = ClipboardEntry(
            preview = rawText,
            timestampMillis = System.currentTimeMillis(),
            patternMatchId = action.patternId,
            patternLabel = if (action.patternId != null) action.label else null,
            matchedText = if (action.patternId != null) action.matchedText else null,
            tags = if (action.patternId != null) listOf("Matched", action.label) else listOf("Plaintext")
        )
        historyRepo.insert(entry)

        // 4. Trigger alert notification on context background
        val showNotifs = ServiceLocator.getPreferencesManager().showNotificationsFlow.first()
        if (showNotifs && action.patternId != null) {
            withContext(Dispatchers.Main) {
                notificationHelper.showActionNotification(action)
            }
        }

        action
    }

    private fun stopMonitoringService() {
        processingScope.launch {
            ServiceLocator.getPreferencesManager().setMonitorClipboard(false)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    this@ClipboardMonitorService,
                    "Monitoring paused",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopMonitoringService()
            return START_NOT_STICKY
        } else if (intent?.action == ACTION_PROCESS_TEXT) {
            val text = intent.getStringExtra(EXTRA_TEXT)
            if (!text.isNullOrEmpty()) {
                processingScope.launch {
                    processClipboardText(text)
                }
            }
            return START_STICKY
        }
        // Sticky configuration ensures OS restarts this when memory frees up
        return START_STICKY
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP_MONITORING = "com.michael.clipvault.ACTION_STOP_MONITORING"
        const val ACTION_PROCESS_TEXT = "com.michael.clipvault.ACTION_PROCESS_TEXT"
        const val EXTRA_TEXT = "extra_text"
    }
}
