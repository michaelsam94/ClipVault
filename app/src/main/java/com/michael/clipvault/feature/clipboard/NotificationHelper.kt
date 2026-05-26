package com.michael.clipvault.feature.clipboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.michael.clipvault.MainActivity
import com.michael.clipvault.core.domain.ClipboardAction

class NotificationHelper(private val context: Context) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Background Service (Silent)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Clipboard Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the ClipVault background monitor running smoothly."
                setShowBadge(false)
            }
            nm.createNotificationChannel(serviceChannel)

            // Channel 2: Action Prompts (High importance)
            val actionChannel = NotificationChannel(
                CHANNEL_ACTIONS,
                "Smart Regex Actions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when copied text matches your automation triggers."
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(actionChannel)
        }
    }

    fun buildServiceNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, ClipboardMonitorService::class.java).apply {
            action = ClipboardMonitorService.ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            200,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle("ClipVault Active")
            .setContentText("Monitoring clipboard for patterns locally...")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                stopPendingIntent
            )
            .build()
    }

    fun showActionNotification(action: ClipboardAction) {
        val id = action.notificationId

        // Build Intent for the match-specific action (Direct link, web browse, etc.)
        val actionIntent = when (action.actionType) {
            "OPEN_URL" -> {
                val formattedUrl = action.actionPayload.replace(
                    "{group0}", Uri.encode(action.matchedText)
                ).replace(
                    "{group1}", Uri.encode(action.groups.getOrElse(1) { "" })
                ).replace(
                    "{group2}", Uri.encode(action.groups.getOrElse(2) { "" })
                )
                Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            "TRANSFORM" -> {
                // Launch MainActivity or trigger receiver to run formatting
                Intent(context, MainActivity::class.java).apply {
                    putExtra("EXTRA_ACTION_LAUNCH", "TRANSFORM")
                    putExtra("EXTRA_TEXT_INPUT", action.originalText)
                    putExtra("EXTRA_TRANS_TYPE", action.actionPayload)
                }
            }
            else -> {
                // Default to sharing / app
                Intent(context, MainActivity::class.java)
            }
        }

        val pendingActionIntent = PendingIntent.getActivity(
            context,
            id,
            actionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIONS)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(action.label)
            .setContentText("Matched: ${action.matchedText}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Matched Pattern:\n${action.matchedText}\n\nTap to execute dynamic automation."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingActionIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                if (action.actionType == "TRANSFORM") "Format" else "Launch",
                pendingActionIntent
            )

        nm.notify(id, builder.build())
    }

    companion object {
        const val CHANNEL_SERVICE = "channel_clipboard_service"
        const val CHANNEL_ACTIONS = "channel_smart_actions"
        const val NOTIFICATION_ID = 5005
    }
}
