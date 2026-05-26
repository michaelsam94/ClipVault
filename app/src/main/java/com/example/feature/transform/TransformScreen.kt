package com.example.feature.transform

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.domain.Transformer

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransformScreen(
    viewModel: TransformViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val input by viewModel.inputText.collectAsStateWithLifecycle()
    val output by viewModel.outputText.collectAsStateWithLifecycle()
    val selectedTransformer by viewModel.selectedTransformer.collectAsStateWithLifecycle()

    val findRegexPattern by viewModel.regexReplacePattern.collectAsStateWithLifecycle()
    val replaceValue by viewModel.regexReplaceValue.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Pipeline Hub", fontWeight = FontWeight.Bold)
                        Text("On-device offline text transforms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Text Field Box
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Input Text", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    TextButton(
                        onClick = {
                            val clipText = context.getSystemService(android.content.ClipboardManager::class.java)
                                .primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.isNotEmpty()) {
                                viewModel.setInputText(clipText)
                                Toast.makeText(context, "Pasted into input", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "System clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste from Clipboard", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = { Text("Enter string or source text to automate transforms offline.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Selector Chips flow
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Processor Transformer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    Transformer.ALL.forEach { trans ->
                        val isSelected = selectedTransformer.javaClass == trans.javaClass
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTransformer(trans) },
                            label = { Text(trans.label) }
                        )
                    }
                    
                    // Regex Replace Filter option
                    val isRegexReplaceSelected = selectedTransformer is Transformer.RegexReplace
                    FilterChip(
                        selected = isRegexReplaceSelected,
                        onClick = {
                            viewModel.selectTransformer(Transformer.RegexReplace(findRegexPattern, replaceValue))
                        },
                        label = { Text("Regex Replace") }
                    )
                }
            }

            // If Regex Replace selected: Custom settings parameters fields
            AnimatedVisibility(
                visible = selectedTransformer is Transformer.RegexReplace,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Regex Custom Replacement Options", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = findRegexPattern,
                                onValueChange = { viewModel.updateRegexReplaceParams(it, replaceValue) },
                                label = { Text("Find Regex Pattern") },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                            OutlinedTextField(
                                value = replaceValue,
                                onValueChange = { viewModel.updateRegexReplaceParams(findRegexPattern, it) },
                                label = { Text("Replace String") },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                        }
                    }
                }
            }

            // Results UI Column Output text box
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Result Output", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    if (output.isNotEmpty()) {
                        Row {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(output))
                                    Toast.makeText(context, "Copied formatted result", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy result", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, output)
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share pipeline result via"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share result", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    if (output.isEmpty()) {
                        Text(
                            "Transformation results appear here automatically...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = output,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
