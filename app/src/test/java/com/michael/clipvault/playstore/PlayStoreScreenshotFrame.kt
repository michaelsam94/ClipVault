package com.michael.clipvault.playstore

import androidx.compose.runtime.Composable
import com.michael.clipvault.feature.clipboard.HistoryVaultScreen
import com.michael.clipvault.feature.clipboard.ClipboardHistoryViewModel
import com.michael.clipvault.feature.patterns.PatternsScreen
import com.michael.clipvault.feature.patterns.PatternsViewModel
import com.michael.clipvault.feature.patterns.PlaygroundTabContent
import com.michael.clipvault.feature.transform.TransformScreen
import com.michael.clipvault.feature.transform.TransformViewModel
import com.michael.clipvault.feature.settings.SettingsScreen
import com.michael.clipvault.ui.theme.MyApplicationTheme

enum class PlayStoreScene {
    Dashboard,
    PatternsRules,
    PatternsPlayground,
    Transform,
    Settings
}

@Composable
fun PlayStoreScreenshotFrame(
    scene: PlayStoreScene,
    historyViewModel: ClipboardHistoryViewModel = ClipboardHistoryViewModel(),
    patternsViewModel: PatternsViewModel = PatternsViewModel(),
    transformViewModel: TransformViewModel = TransformViewModel()
) {
    MyApplicationTheme {
        when (scene) {
            PlayStoreScene.Dashboard -> {
                HistoryVaultScreen(
                    viewModel = historyViewModel,
                    onActiveScanTriggered = {}
                )
            }
            PlayStoreScene.PatternsRules -> {
                PatternsScreen(
                    viewModel = patternsViewModel
                )
            }
            PlayStoreScene.PatternsPlayground -> {
                // PlaygroundTabContent is public in Kotlin by default
                PlaygroundTabContent(
                    viewModel = patternsViewModel
                )
            }
            PlayStoreScene.Transform -> {
                TransformScreen(
                    viewModel = transformViewModel
                )
            }
            PlayStoreScene.Settings -> {
                SettingsScreen(
                    onHistoryWiped = {}
                )
            }
        }
    }
}
