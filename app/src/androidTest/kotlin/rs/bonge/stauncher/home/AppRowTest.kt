package rs.bonge.stauncher.home

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import android.view.KeyEvent as AndroidKeyEvent

@RunWith(AndroidJUnit4::class)
class AppRowTest {
    @get:Rule
    val rule = createComposeRule()

    // The packages do not exist, so no art loads and every card is an icon-only card with its name.
    private val bannerLoader =
        BannerLoader(InstrumentationRegistry.getInstrumentation().targetContext.packageManager)

    private var apps by mutableStateOf(testApps(8))
    private val launched = mutableListOf<String>()

    private fun testApps(count: Int) =
        (1..count).map {
            InstalledApp(
                packageName = "test.app$it",
                className = "test.app$it.Main",
                label = "App $it",
                leanback = true,
                hasBanner = false,
                firstInstallTime = it.toLong(),
                lastUpdateTime = it.toLong(),
            )
        }

    private fun showRow() {
        rule.setContent {
            AppRow(
                apps = apps,
                bannerLoader = bannerLoader,
                onLaunch = { launched += it.packageName },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        rule.waitForIdle()
    }

    /** One key press, with [repeats] auto-repeat events in between like a held key. */
    private fun press(
        keyCode: Int,
        repeats: Int = 0,
    ) {
        val time = SystemClock.uptimeMillis()
        for (repeat in 0..repeats) {
            val down = AndroidKeyEvent(time, time, AndroidKeyEvent.ACTION_DOWN, keyCode, repeat)
            rule.onRoot().performKeyPress(KeyEvent(down))
        }
        rule.onRoot().performKeyPress(KeyEvent(AndroidKeyEvent(time, time, AndroidKeyEvent.ACTION_UP, keyCode, 0)))
        rule.waitForIdle()
    }

    private fun assertFocused(label: String) {
        val focused =
            rule
                .onAllNodes(isFocused())
                .fetchSemanticsNodes()
                .map { it.config[SemanticsProperties.ContentDescription].single() }
        assertEquals(listOf(label), focused)
    }

    @Test
    fun `focuses the first app when the row appears`() {
        showRow()

        assertFocused("App 1")
    }

    @Test
    fun `moves focus along the row`() {
        showRow()

        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertFocused("App 2")
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertFocused("App 3")
        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
        assertFocused("App 2")
    }

    @Test
    fun `holds the focused app in the pivot slot while the row scrolls`() {
        showRow()

        repeat(3) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }

        assertFocused("App 4")
        rule.onNodeWithContentDescription("App 4").assertLeftPositionInRootIsEqualTo(48.dp)
    }

    @Test
    fun `clamps the scroll at the end of the row`() {
        showRow()

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)

        assertFocused("App 8")
        // 8 cards of 192 dp with 16 dp gaps and 48 dp padding make a 1744 dp row in a 960 dp viewport.
        rule.onNodeWithContentDescription("App 8").assertLeftPositionInRootIsEqualTo(960.dp - 48.dp - 192.dp)
    }

    @Test
    fun `wraps from the first app to the last on a fresh left press`() {
        showRow()

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)

        assertFocused("App 8")
    }

    @Test
    fun `wraps from the last app to the first on a fresh right press`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
        assertFocused("App 8")

        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)

        assertFocused("App 1")
    }

    @Test
    fun `does not wrap while the key is held`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertFocused("App 2")

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT, repeats = 3)

        assertFocused("App 1")
    }

    @Test
    fun `launches the focused app on select`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("test.app2"), launched)
    }

    @Test
    fun `moves focus to the app that takes the place of a removed app`() {
        showRow()
        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }
        assertFocused("App 3")

        rule.runOnIdle { apps = apps.filterNot { it.label == "App 3" } }
        rule.waitForIdle()

        assertFocused("App 4")
    }

    @Test
    fun `moves focus to the new last app when the last app is removed`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
        assertFocused("App 8")

        rule.runOnIdle { apps = apps.dropLast(1) }
        rule.waitForIdle()

        assertFocused("App 7")
    }

    @Test
    fun `keeps focus where it is when an app is added`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertFocused("App 2")

        rule.runOnIdle { apps = apps + testApps(9).last() }
        rule.waitForIdle()

        assertFocused("App 2")
    }
}
