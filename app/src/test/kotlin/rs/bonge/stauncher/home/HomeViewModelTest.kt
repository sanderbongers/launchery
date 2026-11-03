package rs.bonge.stauncher.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import rs.bonge.stauncher.MainDispatcherRule
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.apps.testApp
import rs.bonge.stauncher.layout.Arranged
import rs.bonge.stauncher.layout.Layout
import rs.bonge.stauncher.layout.arrangeApps
import rs.bonge.stauncher.settings.AppStyle
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val a = testApp("a", firstInstallTime = 1)
    private val b = testApp("b", firstInstallTime = 2)
    private val c = testApp("c", firstInstallTime = 3)

    private val installed = MutableStateFlow(listOf(a, b, c))
    private val layout = MutableStateFlow(Layout())
    private val appStyle = MutableStateFlow(AppStyle.RoundIcons)
    private val saved = mutableListOf<Layout>()

    // Saving feeds back into the layout, like the real repository does.
    private fun TestScope.viewModel(
        save: suspend (Layout) -> Unit = {
            saved += it
            layout.value = it
        },
    ): HomeViewModel {
        val viewModel =
            HomeViewModel(combine(installed, layout, ::arrangeApps), appStyle, save) {
                appStyle.value =
                    appStyle.value.next()
            }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun HomeViewModel.packages() = uiState.value.apps.map { it.packageName }

    @Test
    fun `is not loaded until the apps and the layout are known`() =
        runTest {
            val viewModel = HomeViewModel(MutableSharedFlow<Arranged>(), appStyle, {}, {})
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertEquals(HomeUiState(), viewModel.uiState.value)
        }

    @Test
    fun `follows the style the user chose`() =
        runTest {
            appStyle.value = AppStyle.Banners
            val viewModel = viewModel()
            assertEquals(AppStyle.Banners, viewModel.uiState.value.appStyle)

            appStyle.value = AppStyle.RoundIcons

            assertEquals(AppStyle.RoundIcons, viewModel.uiState.value.appStyle)
        }

    @Test
    fun `is not loaded until the style is known, so the row never shows the wrong style first`() =
        runTest {
            val viewModel = HomeViewModel(combine(installed, layout, ::arrangeApps), emptyFlow(), {}, {})
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertEquals(HomeUiState(), viewModel.uiState.value)
        }

    @Test
    fun `lists the hidden apps by name, ignoring case, not by their slot`() =
        runTest {
            val beta = testApp("p.1", label = "beta")
            val alpha = testApp("p.2", label = "Alpha")
            val zulu = testApp("p.3", label = "Zulu")
            installed.value = listOf(beta, alpha, zulu)
            layout.value = Layout(order = listOf("p.3", "p.1", "p.2"), hidden = setOf("p.1", "p.2", "p.3"))

            val viewModel = viewModel()

            assertEquals(listOf(alpha, beta, zulu), viewModel.uiState.value.hidden)
            assertTrue(viewModel.packages().isEmpty())
        }

    @Test
    fun `unhides an app and saves the layout`() =
        runTest {
            layout.value = Layout(order = listOf("a", "b", "c"), hidden = setOf("b"))
            val viewModel = viewModel()
            assertEquals(listOf(b), viewModel.uiState.value.hidden)

            viewModel.unhide(b)

            assertEquals(listOf(Layout(order = listOf("a", "b", "c"), hidden = emptySet())), saved)
            assertTrue(
                viewModel.uiState.value.hidden
                    .isEmpty(),
            )
            assertEquals(listOf("a", "b", "c"), viewModel.packages())
        }

    @Test
    fun `switches between the two app styles`() =
        runTest {
            val viewModel = viewModel()
            assertEquals(AppStyle.RoundIcons, viewModel.uiState.value.appStyle)

            viewModel.toggleAppStyle()
            assertEquals(AppStyle.Banners, viewModel.uiState.value.appStyle)

            viewModel.toggleAppStyle()
            assertEquals(AppStyle.RoundIcons, viewModel.uiState.value.appStyle)
        }

    @Test
    fun `opens the settings and closes them again`() =
        runTest {
            val viewModel = viewModel()

            viewModel.openSettings()
            assertEquals(HomeMode.Settings, viewModel.uiState.value.mode)

            viewModel.dismiss()
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `puts the settings away when the user goes home`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openSettings()
            val requests = viewModel.uiState.value.homeRequests

            viewModel.goHome()

            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
            assertEquals(requests + 1, viewModel.uiState.value.homeRequests)
        }

    @Test
    fun `shows the visible apps in their arranged order`() =
        runTest {
            layout.value = Layout(order = listOf("c", "a", "b"), hidden = setOf("a"))
            val viewModel = viewModel()

            assertTrue(viewModel.uiState.value.loaded)
            assertEquals(listOf("c", "b"), viewModel.packages())
        }

    @Test
    fun `puts an app installed later at the end`() =
        runTest {
            val viewModel = viewModel()

            installed.value = listOf(a, b, c, testApp("z", firstInstallTime = 5))

            assertEquals(listOf("a", "b", "c", "z"), viewModel.packages())
        }

    @Test
    fun `observes the apps only while the UI collects the state`() =
        runTest {
            val upstream = MutableSharedFlow<Arranged>()
            val viewModel = HomeViewModel(upstream, appStyle, {}, {})
            assertEquals(0, upstream.subscriptionCount.value)

            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            assertEquals(1, upstream.subscriptionCount.value)

            collector.cancel()
            runCurrent()
            assertEquals(0, upstream.subscriptionCount.value)
        }

    @Test
    fun `opens the actions for an app and closes them again`() =
        runTest {
            val viewModel = viewModel()

            viewModel.openActions(b)
            assertEquals(HomeMode.Actions(b), viewModel.uiState.value.mode)

            viewModel.dismiss()
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `counts each time the user goes home`() =
        runTest {
            val viewModel = viewModel()
            assertEquals(0, viewModel.uiState.value.homeRequests)

            viewModel.goHome()
            viewModel.goHome()

            assertEquals(2, viewModel.uiState.value.homeRequests)
        }

    @Test
    fun `closes the actions without acting when the user goes home`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(b)

            viewModel.goHome()

            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
            assertTrue(saved.isEmpty())
        }

    @Test
    fun `puts a picked-up app back without saving when the user goes home`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(a)
            viewModel.startMove()
            viewModel.moveBy(1)

            viewModel.goHome()

            assertEquals(listOf("a", "b", "c"), viewModel.packages())
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
            assertTrue(saved.isEmpty())
        }

    @Test
    fun `closes the actions when their app disappears`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(b)

            installed.value = listOf(a, c)

            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `hides the app and saves the layout`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(b)

            viewModel.hide()

            assertEquals(listOf(Layout(order = listOf("a", "b", "c"), hidden = setOf("b"))), saved)
            assertEquals(listOf("a", "c"), viewModel.packages())
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `picks up an app and previews each step without saving`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(a)
            viewModel.startMove()
            assertEquals(HomeMode.Moving(a), viewModel.uiState.value.mode)

            viewModel.moveBy(1)
            assertEquals(listOf("b", "a", "c"), viewModel.packages())
            viewModel.moveBy(1)
            assertEquals(listOf("b", "c", "a"), viewModel.packages())
            assertTrue(saved.isEmpty())
        }

    @Test
    fun `stops at the ends of the row instead of wrapping`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(a)
            viewModel.startMove()

            viewModel.moveBy(-1)
            assertEquals(listOf("a", "b", "c"), viewModel.packages())

            repeat(5) { viewModel.moveBy(1) }
            assertEquals(listOf("b", "c", "a"), viewModel.packages())
        }

    @Test
    fun `saves the new order when the app is dropped`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(a)
            viewModel.startMove()
            viewModel.moveBy(1)

            viewModel.drop()

            assertEquals(listOf(Layout(order = listOf("b", "a", "c"))), saved)
            assertEquals(listOf("b", "a", "c"), viewModel.packages())
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `puts the app back and saves nothing when the move is cancelled`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(a)
            viewModel.startMove()
            viewModel.moveBy(1)

            viewModel.dismiss()

            assertTrue(saved.isEmpty())
            assertEquals(listOf("a", "b", "c"), viewModel.packages())
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `cannot pick up the only app`() =
        runTest {
            installed.value = listOf(a)
            val viewModel = viewModel()
            viewModel.openActions(a)

            viewModel.startMove()

            assertEquals(HomeMode.Actions(a), viewModel.uiState.value.mode)
        }

    @Test
    fun `keeps a picked-up app and puts an app that arrives meanwhile at the end`() =
        runTest {
            val viewModel = viewModel()
            viewModel.openActions(a)
            viewModel.startMove()
            viewModel.moveBy(1)

            installed.value = listOf(a, b, c, testApp("z", firstInstallTime = 5))

            assertEquals(listOf("b", "a", "c", "z"), viewModel.packages())
            assertEquals(HomeMode.Moving(a), viewModel.uiState.value.mode)
        }

    @Test
    fun `leaves the layout as it was when saving fails`() =
        runTest {
            val viewModel = viewModel(save = { throw IOException("disk full") })
            viewModel.openActions(b)

            viewModel.hide()

            assertEquals(listOf("a", "b", "c"), viewModel.packages())
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }

    @Test
    fun `ignores move, drop and hide when nothing is open`() =
        runTest {
            val viewModel = viewModel()

            viewModel.startMove()
            viewModel.moveBy(1)
            viewModel.drop()
            viewModel.hide()

            assertTrue(saved.isEmpty())
            assertEquals(HomeMode.Browsing, viewModel.uiState.value.mode)
        }
}
