package com.michael.clipvault.feature.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michael.clipvault.core.domain.ClipboardEntry
import com.michael.clipvault.ui.theme.ClipVaultTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryVaultScreen(
    viewModel: ClipboardHistoryViewModel,
    modifier: Modifier = Modifier,
    onActiveScanTriggered: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rawHistory by viewModel.historyState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var activeFilter by remember { mutableStateOf("ALL") } // "ALL", "FAVS", "MATCHES"
    
    val filteredHistory = remember(rawHistory, activeFilter) {
        when (activeFilter) {
            "FAVS" -> rawHistory.filter { it.isFavourite }
            "MATCHES" -> rawHistory.filter { it.patternMatchId != null }
            else -> rawHistory
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "History Vault",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            "Securely locked local clipboard snippets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (filteredHistory.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.clearAllHistory()
                            Toast.makeText(context, "History wiped securely", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Manual capture (Great override trigger for newer Android background limits!)
                    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = sysClipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val txt = clip.getItemAt(0).coerceToText(context)?.toString() ?: ""
                        if (txt.isNotEmpty()) {
                            onActiveScanTriggered(txt)
                            Toast.makeText(context, "Scanning manual clipboard text...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No clip data available", Toast.LENGTH_SHORT).show()
                    }
                },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                text = { Text("Scan System Clipboard") },
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text("Search decrypted titles, tags, previews...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear text")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Dynamic filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeFilter == "ALL",
                    onClick = { activeFilter = "ALL" },
                    label = { Text("All (${rawHistory.size})") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeFilter == "FAVS",
                    onClick = { activeFilter = "FAVS" },
                    label = { Text("Favorites (${rawHistory.count { it.isFavourite }})") },
                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = activeFilter == "MATCHES",
                    onClick = { activeFilter = "MATCHES" },
                    label = { Text("Matches (${rawHistory.count { it.patternMatchId != null }})") },
                    leadingIcon = { Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Outlined.ContentPasteOff,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found" else "Vault is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try checking the spelling or query parameters." else "Copy some text in any app, or use manual Scan below!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredHistory,
                        key = { it.id }
                    ) { entry ->
                        ClipboardEntryItem(
                            entry = entry,
                            onToggleFavorite = { viewModel.toggleFavourite(entry.id) },
                            onDelete = { viewModel.deleteEntry(entry.id) },
                            onDecryptTrigger = { viewModel.decryptEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardEntryItem(
    entry: ClipboardEntry,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDecryptTrigger: suspend () -> String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var decryptedText by remember { mutableStateOf<String?>(null) }
    var ivDecryptionLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sysClipboard = LocalClipboardManager.current

    val simpleDateFormat = remember { SimpleDateFormat("MMMM dd, hh:mm a", Locale.getDefault()) }
    val dateString = remember(entry.timestampMillis) { simpleDateFormat.format(Date(entry.timestampMillis)) }

    val hasRegexMatch = entry.patternMatchId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasRegexMatch) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Preview and expandable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val textToUse = decryptedText ?: entry.preview
                val isUrl = remember(textToUse) {
                    textToUse.startsWith("http://") || 
                    textToUse.startsWith("https://") || 
                    textToUse.startsWith("www.") ||
                    android.util.Patterns.WEB_URL.matcher(textToUse).matches()
                }
                val isEmail = remember(textToUse) {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(textToUse).matches()
                }
                val isPhone = remember(textToUse) {
                    android.util.Patterns.PHONE.matcher(textToUse).matches()
                }
                val isClickableAction = isUrl || isEmail || isPhone

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Date/Time
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (entry.isFavourite) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorit",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text display
                    Text(
                        text = textToUse,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (isClickableAction) androidx.compose.ui.text.style.TextDecoration.Underline else null
                        ),
                        fontFamily = if (decryptedText != null) FontFamily.Monospace else FontFamily.SansSerif,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isClickableAction) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.then(
                            if (isClickableAction) {
                                Modifier.clickable {
                                    val intentUrl = when {
                                        isUrl -> if (textToUse.startsWith("www.")) "https://$textToUse" else textToUse
                                        isEmail -> "mailto:$textToUse"
                                        isPhone -> "tel:$textToUse"
                                        else -> textToUse
                                    }
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                    )
                }

                IconButton(
                    onClick = {
                        isExpanded = !isExpanded
                        if (!isExpanded) {
                            // Reset decryption cache on close to preserve memory/privacy
                            decryptedText = null
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand item details"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges / Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip Tag
                if (hasRegexMatch) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                entry.patternLabel ?: "Matched",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ClipVaultTokens.MatchHighlightOnContainer
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Plaintext") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val textToUse = decryptedText ?: entry.preview
                val isUrl = remember(textToUse) {
                    textToUse.startsWith("http://") || 
                    textToUse.startsWith("https://") || 
                    textToUse.startsWith("www.") ||
                    android.util.Patterns.WEB_URL.matcher(textToUse).matches()
                }
                val isEmail = remember(textToUse) {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(textToUse).matches()
                }
                val isPhone = remember(textToUse) {
                    android.util.Patterns.PHONE.matcher(textToUse).matches()
                }

                if (isUrl) {
                    IconButton(
                        onClick = {
                            val intentUrl = if (textToUse.startsWith("www.")) "https://$textToUse" else textToUse
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to handle this link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Link",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isEmail) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$textToUse")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to send email", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Send Email",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isPhone) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:$textToUse")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to call number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call Number",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Action buttons quick view on expanded
                IconButton(
                    onClick = {
                        sysClipboard.setText(buildAnnotatedString { append(textToUse) })
                        Toast.makeText(context, "Copied snippet", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                }
            }

            // Expanded action box (Secured with AES Keystore decryption)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    Text(
                        "Local Cryptography Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Payload is AES-GCM-256 encrypted using an on-device Master Key stored securely inside Android's Hardware Keystore TEE wrapper.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrypt Button
                        if (decryptedText == null) {
                            Button(
                                onClick = {
                                    ivDecryptionLoading = true
                                    scope.launch {
                                        decryptedText = onDecryptTrigger()
                                        ivDecryptionLoading = false
                                        Toast.makeText(context, "Decrypted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (ivDecryptionLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Decrypt payload", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        } else {
                            Button(
                                onClick = { decryptedText = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-Lock", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }

                        // Toggle Favourite
                        OutlinedButton(
                            onClick = onToggleFavorite,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (entry.isFavourite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (entry.isFavourite) "Unfavourite" else "Favourite",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = onDelete,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
