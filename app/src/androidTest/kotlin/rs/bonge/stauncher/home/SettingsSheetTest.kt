package rs.bonge.stauncher.home

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.settings.AppStyle
import android.view.KeyEvent as AndroidKeyEvent

@RunWith(AndroidJUnit4::class)
class SettingsSheetTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // The packages do not exist, so no icons load; the tiles still show their names.
    private val bannerLoader =
        BannerLoader(InstrumentationRegistry.getInstrumentation().targetContext.packageManager)

    private fun app(number: Int) =
        InstalledApp(
            packageName = "test.app$number",
            className = "test.app$number.Main",
            label = "App $number",
            leanback = true,
            hasBanner = false,
            system = false,
            firstInstallTime = number.toLong(),
            lastUpdateTime = number.toLong(),
        )

    private var hidden by mutableStateOf((1..3).map(::app))
    private var appStyle by mutableStateOf(AppStyle.RoundIcons)
    private val calls = mutableListOf<String>()

    // Unhiding removes the app from the list, like the view model does once the layout is saved.
    private fun showSheet() {
        rule.setContent {
            SettingsSheet(
                hidden = hidden,
                hiddenLoaded = true,
                appStyle = appStyle,
                bannerLoader = bannerLoader,
                onToggleAppStyle = { calls += "toggle style" },
                onUnhide = { app ->
                    calls += "unhide ${app.label}"
                    hidden = hidden - app
                },
                onOpenSystemSettings = { calls += "all settings" },
            )
        }
        rule.waitForIdle()
    }

    private fun press(keyCode: Int) {
        val time = SystemClock.uptimeMillis()
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(time, time, AndroidKeyEvent.ACTION_DOWN, keyCode, 0)))
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(time, time, AndroidKeyEvent.ACTION_UP, keyCode, 0)))
        rule.waitForIdle()
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    /** From the main page: down past the Show apps as tile to the Hidden apps tile, then select. */
    private fun openHiddenApps() {
        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
    }

    @Test
    fun `starts on the All settings button with the tiles below it`() {
        showSheet()

        rule.onNodeWithContentDescription("All settings").assertIsFocused()
        rule.onNodeWithText("All settings").assertIsDisplayed()
        rule.onNodeWithText("Show apps as").assertIsDisplayed()
        rule.onNodeWithText("Hidden apps").assertIsDisplayed()
        assertTrue(rule.onAllNodes(hasText("App 1")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `names the style in use and switches it on select`() {
        showSheet()
        rule.onNodeWithText("Round icons").assertIsDisplayed()

        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("toggle style"), calls)
    }

    @Test
    fun `names the banner style when that is in use`() {
        appStyle = AppStyle.Banners
        showSheet()

        rule.onNodeWithText("Banners").assertIsDisplayed()
    }

    @Test
    fun `opens the system settings on select`() {
        showSheet()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("all settings"), calls)
    }

    @Test
    fun `opens the list of hidden apps and focuses the first app`() {
        showSheet()

        openHiddenApps()

        rule.onNodeWithText("App 1").assertIsFocused()
        rule.onNodeWithText("App 3").assertIsDisplayed()
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `says so when no app is hidden`() {
        hidden = emptyList()
        showSheet()

        openHiddenApps()

        rule.onNodeWithText("No hidden apps").assertIsDisplayed()
        rule.onAllNodes(isFocused()).assertCountEquals(0)
    }

    @Test
    fun `unhides the focused app on select and keeps focus at the same place in the list`() {
        showSheet()
        openHiddenApps()
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithText("App 2").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("unhide App 2"), calls)
        rule.onNodeWithText("App 3").assertIsFocused()
    }

    @Test
    fun `moves focus up when the last app of the list is unhidden`() {
        showSheet()
        openHiddenApps()
        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }
        rule.onNodeWithText("App 3").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        rule.onNodeWithText("App 2").assertIsFocused()
    }

    @Test
    fun `says so when the last hidden app is unhidden`() {
        hidden = listOf(app(1))
        showSheet()
        openHiddenApps()
        rule.onNodeWithText("App 1").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        rule.onNodeWithText("No hidden apps").assertIsDisplayed()
    }

    @Test
    fun `goes back from the list to the main page on the Hidden apps tile`() {
        showSheet()
        openHiddenApps()

        back()

        rule.onNodeWithText("Hidden apps").assertIsFocused()
        rule.onNodeWithText("Show apps as").assertIsDisplayed()
        assertTrue(rule.onAllNodes(hasText("App 1")).fetchSemanticsNodes().isEmpty())
    }
}
