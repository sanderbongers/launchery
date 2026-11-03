package rs.bonge.launchery.home

import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rs.bonge.launchery.settings.AppStyle
import android.view.KeyEvent as AndroidKeyEvent

@RunWith(AndroidJUnit4::class)
class AppRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val bannerLoader = testBannerLoader()

    private var apps by mutableStateOf(testApps(8))
    private var homeRequests by mutableIntStateOf(0)
    private val launched = mutableListOf<String>()

    // Eight banner cards are wider than the screen, so the scrolling tests use them; the round icons have their own.
    private fun showRow(style: AppStyle = AppStyle.Banners) {
        rule.setContent {
            AppRow(
                apps = apps,
                style = style,
                mode = HomeMode.Browsing,
                homeRequests = homeRequests,
                bannerLoader = bannerLoader,
                onLaunch = { launched += it.packageName },
                onOpenActions = {},
                onMoveBy = {},
                onDrop = {},
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
        rule.onNodeWithContentDescription("App 4").assertLeftPositionInRootIsEqualTo(58.dp)
    }

    @Test
    fun `clamps the scroll at the end of the row`() {
        showRow()

        scrollToLastApp()

        assertFocused("App 8")
        // 8 cards of 192 dp with 16 dp gaps and 58 dp margins make a 1764 dp row in a 960 dp viewport.
        rule.onNodeWithContentDescription("App 8").assertLeftPositionInRootIsEqualTo(960.dp - 58.dp - 192.dp)
    }

    @Test
    fun `starts the round icons at the margin and puts them 91 dp apart`() {
        showRow(AppStyle.RoundIcons)

        rule.onNodeWithContentDescription("App 1").assertLeftPositionInRootIsEqualTo(58.dp)
        rule.onNodeWithContentDescription("App 2").assertLeftPositionInRootIsEqualTo(58.dp + 91.dp)
    }

    @Test
    fun `holds the focused round icon in the pivot slot while a longer row scrolls`() {
        apps = testApps(14)
        showRow(AppStyle.RoundIcons)

        repeat(3) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }

        assertFocused("App 4")
        rule.onNodeWithContentDescription("App 4").assertLeftPositionInRootIsEqualTo(58.dp)
    }

    /** Holds right from the first app until the last one has focus; the extra repeat shows that it stops there. */
    private fun scrollToLastApp() {
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT, repeats = 8)
        assertFocused("App 8")
    }

    @Test
    fun `does nothing on a fresh left press at the first app`() {
        showRow()

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)

        assertFocused("App 1")
    }

    @Test
    fun `does nothing on a fresh right press at the last app`() {
        showRow()
        scrollToLastApp()

        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)

        assertFocused("App 8")
    }

    @Test
    fun `stops at the end when the key is held down from further along`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertFocused("App 2")

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT, repeats = 3)

        assertFocused("App 1")
    }

    @Test
    fun `breaks through from the first app to the last when left is held there`() {
        showRow()

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT, repeats = 1)

        assertFocused("App 8")
    }

    @Test
    fun `breaks through from the last app to the first when right is held there`() {
        showRow()
        scrollToLastApp()

        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT, repeats = 1)

        assertFocused("App 1")
    }

    @Test
    fun `jumps only once however long the key stays down`() {
        showRow()

        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT, repeats = 6)

        assertFocused("App 8")
    }

    @Test
    fun `needs a new hold to break through again`() {
        showRow()
        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT, repeats = 1)
        assertFocused("App 8")

        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertFocused("App 8")
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT, repeats = 1)

        assertFocused("App 1")
    }

    @Test
    fun `returns to the first app scrolled back to the start when the user goes home`() {
        showRow()
        repeat(4) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }
        assertFocused("App 5")

        rule.runOnIdle { homeRequests++ }
        rule.waitForIdle()

        assertFocused("App 1")
        rule.onNodeWithContentDescription("App 1").assertLeftPositionInRootIsEqualTo(58.dp)
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
        scrollToLastApp()

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
