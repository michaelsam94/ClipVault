package com.michael.clipvault.feature.patterns

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michael.clipvault.core.domain.RegexPattern
import com.michael.clipvault.core.domain.Transformer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsScreen(
    viewModel: PatternsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Rules, 1 = Playground
    val patterns by viewModel.patternsState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Regex Engine", fontWeight = FontWeight.Bold)
                        Text("Configure automation filters or test regular expressions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Rule")
                        }
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
        ) {
            // Tab Header
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Match Triggers (${patterns.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regex Playground")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (selectedTab == 0) {
                    // TAB 0: Active Rules List
                    RulesTabContent(
                        patterns = patterns,
                        onToggleEnabled = { viewModel.toggleEnabled(it) },
                        onDelete = {
                            viewModel.deletePattern(it)
                            Toast.makeText(context, "Rule deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // TAB 1: Live Playground Playground
                    PlaygroundTabContent(viewModel = viewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AddPatternDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, desc, regex, actType, actPayload, prio ->
                if (label.isEmpty() || regex.isEmpty()) {
                    Toast.makeText(context, "Label and Regex are required!", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.upsertPattern(label, desc, regex, actType, actPayload, prio)
                    showAddDialog = false
                    Toast.makeText(context, "Rule successfully integrated!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun RulesTabContent(
    patterns: List<RegexPattern>,
    onToggleEnabled: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (patterns.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No active matching rules", style = MaterialTheme.typography.titleMedium)
                Text("Reset rules in settings, or add custom ones above!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(
                items = patterns,
                key = { it.id }
            ) { pattern ->
                PatternRuleCard(
                    pattern = pattern,
                    onToggleEnabled = { onToggleEnabled(pattern.id) },
                    onDelete = { onDelete(pattern.id) }
                )
            }
        }
    }
}

@Composable
fun PatternRuleCard(
    pattern: RegexPattern,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (pattern.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pattern.label,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (pattern.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Priority Badge
                        Text(
                            text = "Priority ${pattern.priority}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = pattern.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Switch Toggle
                Switch(
                    checked = pattern.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    thumbContent = if (pattern.isEnabled) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inline details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Match Count Analytics Label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${pattern.matchCount} matched clicks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Show config rule"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    // Regex String Row
                    Text("Regex Formula:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(pattern.regex, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trigger Execution Output
                    Text("Action Result:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (pattern.actionType == "OPEN_URL") Icons.Default.OpenInNew else Icons.Default.Construction,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${pattern.actionType}: ${pattern.actionPayload}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Delete Rule Option (Only for custom ones or all if desired)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove rule", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaygroundTabContent(viewModel: PatternsViewModel) {
    val input by viewModel.playgroundInput.collectAsStateWithLifecycle()
    val regex by viewModel.playgroundRegex.collectAsStateWithLifecycle()
    val result by viewModel.playgroundResult.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Sandbox Regex Realtime Checker",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Pattern String Input
        OutlinedTextField(
            value = regex,
            onValueChange = { viewModel.updatePlaygroundRegex(it) },
            label = { Text("Regex Rule Expression") },
            placeholder = { Text("e.g. \\+?[0-9\\s]{7,15}") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Sandbox Input body
        OutlinedTextField(
            value = input,
            onValueChange = { viewModel.updatePlaygroundInput(it) },
            label = { Text("Sandbox Test Text String") },
            placeholder = { Text("Paste sample context to run matching test against...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Sandbox Verification feedback
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (result) {
                    is PlaygroundResult.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    is PlaygroundResult.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    PlaygroundResult.NoMatch -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (result) {
                            is PlaygroundResult.Success -> Icons.Default.CheckCircle
                            is PlaygroundResult.Error -> Icons.Default.Error
                            PlaygroundResult.NoMatch -> Icons.Default.Warning
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (result) {
                            is PlaygroundResult.Success -> MaterialTheme.colorScheme.primary
                            is PlaygroundResult.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (result) {
                            is PlaygroundResult.Success -> "Match found!"
                            is PlaygroundResult.Error -> "Regex syntax layout error"
                            PlaygroundResult.NoMatch -> "No match detected"
                            else -> "Empty expression"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (val res = result) {
                    is PlaygroundResult.Success -> {
                        Text(
                            "Discovered ${res.matches.size} match result(s):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        res.matches.forEachIndexed { index, match ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Match #${index + 1}: \"${match.value}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Range [${match.range.first}..${match.range.last}]",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (match.groups.size > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Captured Capture Groups:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    match.groups.drop(1).forEachIndexed { grpIndex, valStr ->
                                        Text(
                                            "  • groupIndex ${grpIndex + 1}: \"$valStr\"",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is PlaygroundResult.Error -> {
                        Text(
                            res.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    PlaygroundResult.NoMatch -> {
                        Text(
                            "The formula is valid, but zero occurrences inside the textbox sample context triggered. Check casing, spacing, qualifiers, or anchors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PlaygroundResult.Empty -> {
                        Text(
                            "Enter a valid Regex and sample to process matches offline locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatternDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, desc: String, regex: String, actionType: String, actionPayload: String, priority: Int) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var regex by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("50") }
    
    var actType by remember { mutableStateOf("OPEN_URL") } // "OPEN_URL", "TRANSFORM"
    var actPayload by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Matching Rule", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label Title") },
                    placeholder = { Text("e.g. Flight Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Scan airline codes") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = regex,
                    onValueChange = { regex = it },
                    label = { Text("Regex Pattern Syntax") },
                    placeholder = { Text("e.g. [A-Z]{2}\\d{3,4}") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority numeric
                OutlinedTextField(
                    value = priority,
                    onValueChange = { priority = it },
                    label = { Text("Matching Priority (Higher matches first)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Select Choice
                Text("Automation Action Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = actType == "OPEN_URL",
                        onClick = {
                            actType = "OPEN_URL"
                            actPayload = ""
                        },
                        label = { Text("OPEN_URL") }
                    )
                    FilterChip(
                        selected = actType == "TRANSFORM",
                        onClick = {
                            actType = "TRANSFORM"
                            actPayload = "UPPERCASE"
                        },
                        label = { Text("TRANSFORM") }
                    )
                }

                if (actType == "OPEN_URL") {
                    OutlinedTextField(
                        value = actPayload,
                        onValueChange = { actPayload = it },
                        label = { Text("Target URL Template ({group0} replaces target)") },
                        placeholder = { Text("https://google.com/search?q={group0}") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                } else {
                    var isDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = actPayload,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Transformer Rule List") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            Transformer.ALL.forEach { trans ->
                                DropdownMenuItem(
                                    text = { Text(trans.label) },
                                    onClick = {
                                        actPayload = trans.key
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priority.toIntOrNull() ?: 50
                    onConfirm(label, desc, regex, actType, actPayload, p)
                }
            ) {
                Text("Integrate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
