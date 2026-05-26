package com.michael.clipvault.playstore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreScreenshotTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        PlayStoreTestSupport.seedPlayStoreEnvironment(context)
    }

    @After
    fun tearDown() {
        com.michael.clipvault.core.common.ServiceLocator.resetForTesting()
    }

    // --- Phone Screenshots (1080x1920) ---
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_01_dashboard() {
        capturePlayStoreImage("phone/01_dashboard.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Dashboard)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_02_rules() {
        capturePlayStoreImage("phone/02_rules.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.PatternsRules)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_03_playground() {
        capturePlayStoreImage("phone/03_playground.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.PatternsPlayground)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_04_transform() {
        capturePlayStoreImage("phone/04_transform.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Transform)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_05_settings() {
        capturePlayStoreImage("phone/05_settings.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Settings)
        }
    }

    // --- Tablet Screenshots (1600x2560) ---
    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun tablet_01_dashboard() {
        capturePlayStoreImage("tablet/01_dashboard.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Dashboard)
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun tablet_02_rules() {
        capturePlayStoreImage("tablet/02_rules.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.PatternsRules)
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun tablet_03_transform() {
        capturePlayStoreImage("tablet/03_transform.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Transform)
        }
    }
}
