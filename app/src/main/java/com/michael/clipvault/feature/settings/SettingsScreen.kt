package com.michael.clipvault.feature.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.michael.clipvault.core.common.ServiceLocator
import com.michael.clipvault.feature.clipboard.ClipboardMonitorService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onHistoryWiped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { ServiceLocator.getPreferencesManager() }

    val monitorClipboard by preferencesManager.monitorClipboardFlow.collectAsStateWithLifecycle(initialValue = true)
    val showNotifications by preferencesManager.showNotificationsFlow.collectAsStateWithLifecycle(initialValue = true)

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showResetRulesConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Settings", fontWeight = FontWeight.Bold)
                        Text("Customise the local clipboard matching algorithm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Services Status Toggles
            Text("Automations & Listeners", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Row 1: Monitor clipboard
                    ListItem(
                        headlineContent = { Text("Clipboard Monitoring Listener", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Listens for system clipboard copies and scans for matched rules.") },
                        trailingContent = {
                            Switch(
                                checked = monitorClipboard,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        preferencesManager.setMonitorClipboard(isChecked)
                                        // Dynamically start / stop service helper
                                        val intent = Intent(context, ClipboardMonitorService::class.java)
                                        if (isChecked) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                            Toast.makeText(context, "Monitoring started", Toast.LENGTH_SHORT).show()
                                        } else {
                                            context.stopService(intent)
                                            Toast.makeText(context, "Monitoring paused", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Row 2: Show Notifications alert
                    ListItem(
                        headlineContent = { Text("Trigger Action Notifications", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Pops up alert banner tiles with action buttons upon a pattern match.") },
                        trailingContent = {
                            Switch(
                                checked = showNotifications,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        preferencesManager.setShowNotifications(isChecked)
                                        Toast.makeText(context, if (isChecked) "Alert actions enabled" else "Alert actions disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            // Section 2: Android System Permission checks
            Text("Android System Config", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Notification Panel Permissions") },
                        supportingContent = { Text("Ensure notifications are approved to view automation overlay tiles on modern APIs.") },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            // Section 3: Reset Actions
            Text("Maintanence & Rules Preservation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Wipe DB Vault
                Button(
                    onClick = { showWipeConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Secure Wipe", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                // Restore Default Regex patterns
                OutlinedButton(
                    onClick = { showResetRulesConfirmDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Default Rules", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer branding metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ClipVault Security Hub", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("v${com.michael.clipvault.BuildConfig.VERSION_NAME} (100% Offline Local Engine)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Confirmation dialog for Vault Wiping
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Securely Wipe Database Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("Warning: This will destroy your entire clipboard history. Plaintext captures and AES encrypted records will be deleted forever. This operation can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirmDialog = false
                        onHistoryWiped()
                        Toast.makeText(context, "History wiped permanently.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Erase All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Keep Safe")
                }
            }
        )
    }

    // Confirmation dialog for resetting default patterns
    if (showResetRulesConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetRulesConfirmDialog = false },
            title = { Text("Reset Regex Rule Triggers?", fontWeight = FontWeight.Bold) },
            text = { Text("This will pre-populate the 7 default regex rules (IBAN, Coords, Email, URLs, Json matchers, etc.) and overwrite existing matches database list. Custom rules you created will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetRulesConfirmDialog = false
                        scope.launch {
                            val patternRepo = ServiceLocator.getPatternRepository()
                            // Re-insert default ones
                            com.michael.clipvault.core.database.PatternRepositoryImpl.getBuiltInPatterns().forEach {
                                patternRepo.upsert(it)
                            }
                            Toast.makeText(context, "Default rules restored successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Re-validate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetRulesConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
