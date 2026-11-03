package rs.bonge.stauncher.settings

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
import android.view.KeyEvent as AndroidKeyEvent

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // The packages do not exist, so no icons load; the rows still show their names.
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

    private var state by mutableStateOf(SettingsUiState(hidden = (1..3).map(::app), loaded = true))
    private val calls = mutableListOf<String>()

    // Unhiding removes the app from the state, like the view model does once the layout is saved.
    private fun showSettings() {
        rule.setContent {
            SettingsScreen(
                state = state,
                bannerLoader = bannerLoader,
                onUnhide = { app ->
                    calls += "unhide ${app.label}"
                    state = state.copy(hidden = state.hidden - app)
                },
                onToggleAppStyle = { calls += "toggle style" },
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

    /** From the main page: down to the Hidden apps row, then select. */
    private fun openHiddenApps() {
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
    }

    @Test
    fun `starts on the All settings row with the Hidden apps row below it`() {
        showSettings()

        rule.onNodeWithText("All settings").assertIsFocused()
        rule.onNodeWithText("Hidden apps").assertIsDisplayed()
        assertTrue(rule.onAllNodes(hasText("App 1")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `opens the system settings on select`() {
        showSettings()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("all settings"), calls)
    }

    @Test
    fun `names the style the apps are shown in and switches it on select`() {
        showSettings()
        rule.onNodeWithText("Show apps as").assertIsDisplayed()
        rule.onNodeWithText("Round icons").assertIsDisplayed()

        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }
        rule.onNodeWithText("Show apps as").assertIsFocused()
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("toggle style"), calls)
    }

    @Test
    fun `names the banner style when that is in use`() {
        state = state.copy(appStyle = AppStyle.Banners)
        showSettings()

        rule.onNodeWithText("Banners").assertIsDisplayed()
    }

    @Test
    fun `opens the list of hidden apps and focuses the first app`() {
        showSettings()

        openHiddenApps()

        rule.onNodeWithText("App 1").assertIsFocused()
        rule.onNodeWithText("App 3").assertIsDisplayed()
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `says so when no app is hidden`() {
        state = state.copy(hidden = emptyList())
        showSettings()

        openHiddenApps()

        rule.onNodeWithText("No hidden apps").assertIsDisplayed()
        rule.onAllNodes(isFocused()).assertCountEquals(0)
    }

    @Test
    fun `unhides the focused app on select and keeps focus at the same place in the list`() {
        showSettings()
        openHiddenApps()
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithText("App 2").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("unhide App 2"), calls)
        rule.onNodeWithText("App 3").assertIsFocused()
    }

    @Test
    fun `moves focus up when the last app of the list is unhidden`() {
        showSettings()
        openHiddenApps()
        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }
        rule.onNodeWithText("App 3").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        rule.onNodeWithText("App 2").assertIsFocused()
    }

    @Test
    fun `says so when the last hidden app is unhidden`() {
        state = state.copy(hidden = listOf(app(1)))
        showSettings()
        openHiddenApps()
        rule.onNodeWithText("App 1").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        rule.onNodeWithText("No hidden apps").assertIsDisplayed()
    }

    @Test
    fun `goes back from the list to the main page on the Hidden apps row`() {
        showSettings()
        openHiddenApps()

        back()

        rule.onNodeWithText("Hidden apps").assertIsFocused()
        rule.onNodeWithText("All settings").assertIsDisplayed()
        assertTrue(rule.onAllNodes(hasText("App 1")).fetchSemanticsNodes().isEmpty())
    }
}
