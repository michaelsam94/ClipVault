package com.michael.clipvault.feature.clipboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.michael.clipvault.core.common.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. Attempting to start ClipVault monitoring.")
            val goAsync = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ServiceLocator.initialize(context)
                    val enabled = ServiceLocator.getPreferencesManager().monitorClipboardFlow.first()
                    if (enabled) {
                        val serviceIntent = Intent(context, ClipboardMonitorService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed starting service in background boot", e)
                } finally {
                    goAsync.finish()
                }
            }
        }
    }
}
