package rs.bonge.stauncher.home

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.layout.Arranged
import rs.bonge.stauncher.layout.Layout
import rs.bonge.stauncher.layout.arrangeApps
import rs.bonge.stauncher.settings.AppStyle
import android.view.KeyEvent as AndroidKeyEvent

/** The home screen with the real view model on fake data: the row, the actions panel, move mode and the top bar. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // The packages do not exist, so no art loads and the cards are empty.
    private val bannerLoader =
        BannerLoader(InstrumentationRegistry.getInstrumentation().targetContext.packageManager)

    private val installed = MutableStateFlow(testApps(5))
    private val layout = MutableStateFlow(Layout())

    // The banner cards are wider than the round icons, so the tests that slide cards at the edge of the screen use them.
    private val appStyle = MutableStateFlow(AppStyle.Banners)
    private val saved = mutableListOf<Layout>()
    private val calls = mutableListOf<String>()
    private lateinit var viewModel: HomeViewModel

    // App 2 is a system app, so it gets no Uninstall.
    private fun testApps(count: Int) =
        (1..count).map {
            InstalledApp(
                packageName = "test.app$it",
                className = "test.app$it.Main",
                label = "App $it",
                leanback = true,
                hasBanner = false,
                system = it == 2,
                firstInstallTime = it.toLong(),
                lastUpdateTime = it.toLong(),
            )
        }

    private fun showHome(arranged: Flow<Arranged> = combine(installed, layout, ::arrangeApps)) {
        viewModel =
            HomeViewModel(arranged, appStyle) {
                saved += it
                layout.value = it
            }
        rule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val callbacks =
                HomeCallbacks(
                    onLaunch = { calls += "launch ${it.label}" },
                    onOpenActions = viewModel::openActions,
                    onDismiss = viewModel::dismiss,
                    onStartMove = viewModel::startMove,
                    onMoveBy = viewModel::moveBy,
                    onDrop = viewModel::drop,
                    onHide = viewModel::hide,
                    onAppInfo = {
                        viewModel.dismiss()
                        calls += "info ${it.label}"
                    },
                    onUninstall = {
                        viewModel.dismiss()
                        calls += "uninstall ${it.label}"
                    },
                    onOpenSettings = { calls += "settings" },
                    onOpenSystemSettings = { calls += "system settings" },
                )
            HomeScreen(state = state, time = "20:15", bannerLoader = bannerLoader, callbacks = callbacks)
        }
        rule.waitForIdle()
    }

    private fun send(
        action: Int,
        keyCode: Int,
        repeat: Int = 0,
        flags: Int = 0,
    ) {
        val time = SystemClock.uptimeMillis()
        val event = AndroidKeyEvent(time, time, action, keyCode, repeat, 0, 0, 0, flags)
        rule.onRoot().performKeyPress(KeyEvent(event))
        rule.waitForIdle()
    }

    private fun press(keyCode: Int) {
        send(AndroidKeyEvent.ACTION_DOWN, keyCode)
        send(AndroidKeyEvent.ACTION_UP, keyCode)
    }

    /** Holds the key like a long press: the system repeats it, and the first repeat carries the long-press flag. */
    private fun holdSelect() {
        send(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_CENTER)
        send(
            AndroidKeyEvent.ACTION_DOWN,
            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            repeat = 1,
            flags = AndroidKeyEvent.FLAG_LONG_PRESS,
        )
        send(AndroidKeyEvent.ACTION_DOWN, AndroidKeyEvent.KEYCODE_DPAD_CENTER, repeat = 2)
    }

    private fun releaseSelect() = send(AndroidKeyEvent.ACTION_UP, AndroidKeyEvent.KEYCODE_DPAD_CENTER)

    private fun longPressSelect() {
        holdSelect()
        releaseSelect()
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private fun focusedDescriptions() =
        rule
            .onAllNodes(isFocused())
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.single() }

    private fun assertCardFocused(label: String) = assertEquals(listOf(label), focusedDescriptions())

    private fun cardOrder() =
        viewModel.uiState.value.apps
            .map { it.label }

    private fun openActionsFor(cards: Int) {
        repeat(cards) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }
        longPressSelect()
    }

    /** Opens the actions of the app [cards] places along the row and picks it up with Move, the second action. */
    private fun pickUpApp(cards: Int) {
        openActionsFor(cards)
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
    }

    private fun leftOf(label: String) =
        rule
            .onNodeWithContentDescription(label)
            .getUnclippedBoundsInRoot()
            .left

    private fun leftOrNull(label: String) =
        if (rule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isEmpty()) null else leftOf(label)

    /**
     * Moves the picked-up app by [delta] with the clock stopped, and returns where the card [label] is before the move,
     * on the first frame that shows it moving, and after it. A card that is not composed has no place.
     */
    private fun slideOf(
        label: String,
        delta: Int,
    ): Triple<Dp?, Dp?, Dp?> {
        val before = leftOrNull(label)
        // The view model is called directly: the clock is stopped, and a key press needs it to run.
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { viewModel.moveBy(delta) }
        var during = before
        repeat(30) {
            if (during == before) {
                rule.mainClock.advanceTimeByFrame()
                during = leftOrNull(label)
            }
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        return Triple(before, during, leftOrNull(label))
    }

    /** Whether [middle] lies somewhere between [start] and [end], so that a card is on its way and not at either end. */
    private fun onTheWay(
        start: Dp?,
        middle: Dp?,
        end: Dp?,
    ) = start != null && middle != null && end != null && (middle - start).value * (middle - end).value < 0

    @Test
    fun `opens the actions panel for the focused app on a long press`() {
        showHome()

        openActionsFor(cards = 2)

        rule.onNodeWithText("App info").assertIsFocused()
        rule.onNodeWithText("Move").assertIsDisplayed()
        rule.onNodeWithText("Hide").assertIsDisplayed()
        assertEquals(HomeMode.Actions(viewModel.uiState.value.apps[2]), viewModel.uiState.value.mode)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `does not launch the app on a long press`() {
        showHome()

        longPressSelect()

        assertTrue(calls.isEmpty())
    }

    @Test
    fun `offers Uninstall only for an app the user installed`() {
        showHome()

        longPressSelect()
        rule.onNodeWithText("Uninstall").assertIsDisplayed()
        back()
        openActionsFor(cards = 1)

        assertTrue(rule.onAllNodes(hasText("Uninstall")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `keeps the panel open when the press that opened it is released`() {
        showHome()

        holdSelect()
        rule.onNodeWithText("App info").assertIsFocused()
        releaseSelect()

        rule.onNodeWithText("App info").assertIsFocused()
        assertTrue(calls.isEmpty())
        assertTrue(viewModel.uiState.value.mode is HomeMode.Actions)
    }

    @Test
    fun `keeps focus in the panel`() {
        showHome()
        longPressSelect()

        press(AndroidKeyEvent.KEYCODE_DPAD_UP)
        press(AndroidKeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithText("App info").assertIsFocused()
        repeat(5) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }

        rule.onNodeWithText("Uninstall").assertIsFocused()
    }

    @Test
    fun `closes the panel with Back and gives focus back to the card`() {
        showHome()
        openActionsFor(cards = 1)

        back()

        assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        assertCardFocused("App 2")
    }

    @Test
    fun `runs App info and Uninstall for the app`() {
        showHome()
        longPressSelect()
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
        longPressSelect()
        repeat(3) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("info App 1", "uninstall App 1"), calls)
    }

    @Test
    fun `hides the app and focuses the app that takes its place`() {
        showHome()
        openActionsFor(cards = 1)
        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_DOWN) }
        rule.onNodeWithText("Hide").assertIsFocused()

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("App 1", "App 3", "App 4", "App 5"), cardOrder())
        assertCardFocused("App 3")
        assertEquals(setOf("test.app2"), saved.single().hidden)
    }

    @Test
    fun `moves an app along the row and saves the order when it is dropped`() {
        showHome()
        pickUpApp(cards = 1)
        rule.onNodeWithText("◀ ▶ move · OK place · Back cancel").assertIsDisplayed()

        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(listOf("App 1", "App 3", "App 4", "App 2", "App 5"), cardOrder())
        assertTrue(saved.isEmpty())
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("test.app1", "test.app3", "test.app4", "test.app2", "test.app5"), saved.single().order)
        assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        assertCardFocused("App 2")
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `slides the neighbour into the place the moved app leaves`() {
        showHome()
        pickUpApp(cards = 1)

        val (before, during, after) = slideOf("App 3", delta = 1)

        // The first frame that shows the move is somewhere on the way, not already at the end.
        assertTrue("App 3 went from $before through $during to $after", onTheWay(before, during, after))
    }

    // The row of twelve apps is wider than the screen. The moved app is first in view, so the row jumps a place with
    // every move and its cards slide, including the one that comes into view or leaves it at the right edge.
    @Test
    fun `slides in the card that comes into view at the right edge`() {
        installed.value = testApps(12)
        showHome()
        pickUpApp(cards = 0)
        repeat(3) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }

        val (before, during, after) = slideOf("App 9", delta = 1)

        assertTrue("App 9 went from $before through $during to $after", onTheWay(before, during, after))
    }

    @Test
    fun `slides out the card that leaves the view at the right edge`() {
        installed.value = testApps(12)
        showHome()
        pickUpApp(cards = 0)
        repeat(6) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }

        val (before, during, after) = slideOf("App 11", delta = -1)

        assertTrue("App 11 went from $before through $during to $after", onTheWay(before, during, after))
    }

    @Test
    fun `cancels a move with Back`() {
        showHome()
        pickUpApp(cards = 1)
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)

        back()

        assertEquals(listOf("App 1", "App 2", "App 3", "App 4", "App 5"), cardOrder())
        assertTrue(saved.isEmpty())
        assertCardFocused("App 2")
    }

    @Test
    fun `reaches the settings button with up and returns to the same card with down`() {
        showHome()
        repeat(2) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }
        assertCardFocused("App 3")

        press(AndroidKeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithContentDescription("Settings").assertIsFocused()
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)

        assertCardFocused("App 3")
    }

    @Test
    fun `opens the Stauncher settings on select and the system settings on a long press`() {
        showHome()
        press(AndroidKeyEvent.KEYCODE_DPAD_UP)

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
        longPressSelect()

        assertEquals(listOf("settings", "system settings"), calls)
    }

    @Test
    fun `puts the settings button at the left margin and the time at the right margin`() {
        showHome()

        rule.onNodeWithText("20:15").assertIsDisplayed()
        val screen = rule.onRoot().getUnclippedBoundsInRoot()
        val time = rule.onNodeWithText("20:15").getUnclippedBoundsInRoot()
        val settings = rule.onNodeWithContentDescription("Settings").getUnclippedBoundsInRoot()
        assertEquals(58f, settings.left.value, 0.5f)
        assertEquals((screen.right - 58.dp).value, time.right.value, 0.5f)
        assertTrue("the button ends at ${settings.right}, the time starts at ${time.left}", settings.right <= time.left)
    }

    @Test
    fun `shows the Apps heading above the row and in line with its first app`() {
        showHome()

        rule.onNodeWithText("Apps").assertIsDisplayed()
        val heading = rule.onNodeWithText("Apps").getUnclippedBoundsInRoot()
        val firstApp = rule.onNodeWithContentDescription("App 1").getUnclippedBoundsInRoot()
        assertEquals(58f, heading.left.value, 0.5f)
        assertTrue(
            "the heading ends at ${heading.bottom}, the row starts at ${firstApp.top}",
            heading.bottom <= firstApp.top,
        )
    }

    @Test
    fun `names every app below its icon in the round style`() {
        appStyle.value = AppStyle.RoundIcons
        showHome()

        (1..5).forEach { rule.onNodeWithText("App $it", useUnmergedTree = true).assertIsDisplayed() }
    }

    @Test
    fun `names only the focused card in the banner style`() {
        showHome()

        rule.onNodeWithText("App 1", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("App 2", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `does nothing on Back from the home screen`() {
        showHome()
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)

        back()

        assertFalse(rule.activity.isFinishing)
        assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        assertCardFocused("App 2")
    }

    private fun goHome() {
        rule.runOnUiThread { viewModel.goHome() }
        rule.waitForIdle()
    }

    @Test
    fun `focuses the first app when the user goes home`() {
        showHome()
        repeat(3) { press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT) }
        assertCardFocused("App 4")

        goHome()

        assertCardFocused("App 1")
    }

    @Test
    fun `goes home from the settings button too`() {
        showHome()
        press(AndroidKeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithContentDescription("Settings").assertIsFocused()

        goHome()

        assertCardFocused("App 1")
    }

    @Test
    fun `closes the actions panel without acting when the user goes home`() {
        showHome()
        openActionsFor(cards = 2)

        goHome()

        assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        assertCardFocused("App 1")
        assertTrue(calls.isEmpty())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `puts a picked-up app back without saving when the user goes home`() {
        showHome()
        pickUpApp(cards = 1)
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(listOf("App 1", "App 3", "App 2", "App 4", "App 5"), cardOrder())

        goHome()

        assertEquals(listOf("App 1", "App 2", "App 3", "App 4", "App 5"), cardOrder())
        assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        assertCardFocused("App 1")
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `keeps focus off the settings button until the apps have loaded`() {
        val arranged = MutableSharedFlow<Arranged>(replay = 1)
        showHome(arranged)

        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue(focusedDescriptions().isEmpty())
        arranged.tryEmit(arrangeApps(installed.value, Layout()))
        rule.waitForIdle()

        assertCardFocused("App 1")
    }

    @Test
    fun `shows a message and focuses the settings button when every app is hidden`() {
        layout.value = Layout(hidden = installed.value.map { it.packageName }.toSet())
        showHome()

        rule.onNodeWithText("No apps to show").assertIsDisplayed()
        rule.onNodeWithContentDescription("Settings").assertIsFocused()
    }

    @Test
    fun `shows the message when no app is installed`() {
        installed.value = emptyList()
        showHome()

        rule.onNodeWithText("No apps to show").assertIsDisplayed()
    }
}
