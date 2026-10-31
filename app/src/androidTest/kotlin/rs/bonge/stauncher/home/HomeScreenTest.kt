package rs.bonge.stauncher.home

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.layout.Layout
import rs.bonge.stauncher.layout.arrangeApps
import android.view.KeyEvent as AndroidKeyEvent

/** The home screen with the real view model on fake data: the row, the actions panel, move mode and the top bar. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // The packages do not exist, so no art loads and every card is an icon-only card with its name.
    private val bannerLoader =
        BannerLoader(InstrumentationRegistry.getInstrumentation().targetContext.packageManager)

    // App 2 is a system app, so it gets no Uninstall.
    private val installed =
        MutableStateFlow(
            (1..5).map {
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
            },
        )
    private val layout = MutableStateFlow(Layout())
    private val saved = mutableListOf<Layout>()
    private val calls = mutableListOf<String>()
    private lateinit var viewModel: HomeViewModel

    private fun showHome() {
        viewModel =
            HomeViewModel(combine(installed, layout, ::arrangeApps)) {
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
            HomeScreen(state = state, bannerLoader = bannerLoader, callbacks = callbacks)
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

    @Test
    fun `opens the actions panel for the focused app on a long press`() {
        showHome()

        openActionsFor(cards = 2)

        rule.onNodeWithText("Move").assertIsFocused()
        rule.onNodeWithText("Hide").assertIsDisplayed()
        rule.onNodeWithText("App info").assertIsDisplayed()
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
        rule.onNodeWithText("Move").assertIsFocused()
        releaseSelect()

        rule.onNodeWithText("Move").assertIsFocused()
        assertTrue(calls.isEmpty())
        assertTrue(viewModel.uiState.value.mode is HomeMode.Actions)
    }

    @Test
    fun `keeps focus in the panel`() {
        showHome()
        longPressSelect()

        press(AndroidKeyEvent.KEYCODE_DPAD_UP)
        press(AndroidKeyEvent.KEYCODE_DPAD_UP)
        rule.onNodeWithText("Move").assertIsFocused()
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
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)
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
        press(AndroidKeyEvent.KEYCODE_DPAD_DOWN)

        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals(listOf("App 1", "App 3", "App 4", "App 5"), cardOrder())
        assertCardFocused("App 3")
        assertEquals(setOf("test.app2"), saved.single().hidden)
    }

    @Test
    fun `moves an app along the row and saves the order when it is dropped`() {
        showHome()
        openActionsFor(cards = 1)
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
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
    fun `cancels a move with Back`() {
        showHome()
        openActionsFor(cards = 1)
        press(AndroidKeyEvent.KEYCODE_DPAD_CENTER)
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
