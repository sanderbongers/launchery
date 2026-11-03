package rs.bonge.stauncher.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import rs.bonge.stauncher.MainDispatcherRule
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.apps.testApp
import rs.bonge.stauncher.layout.Layout
import rs.bonge.stauncher.layout.arrangeApps

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val a = testApp("a", firstInstallTime = 1)
    private val b = testApp("b", firstInstallTime = 2)

    private val layout = MutableStateFlow(Layout(order = listOf("a", "b"), hidden = setOf("b")))
    private val appStyle = MutableStateFlow(AppStyle.RoundIcons)
    private val saved = mutableListOf<Layout>()

    // Toggling feeds back into the style, like the real repository does.
    private fun TestScope.viewModel(apps: List<InstalledApp> = listOf(a, b)): SettingsViewModel {
        val arranged = combine(MutableStateFlow(apps), layout, ::arrangeApps)
        val viewModel =
            SettingsViewModel(
                arranged,
                appStyle,
                saveLayout = {
                    saved += it
                    layout.value = it
                },
                saveAppStyleToggle = { appStyle.value = appStyle.value.next() },
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `lists the hidden apps`() =
        runTest {
            val state = viewModel().uiState.value

            assertTrue(state.loaded)
            assertEquals(listOf(b), state.hidden)
        }

    @Test
    fun `lists the hidden apps by name, ignoring case, not by their slot`() =
        runTest {
            val beta = testApp("p.1", label = "beta")
            val alpha = testApp("p.2", label = "Alpha")
            val zulu = testApp("p.3", label = "Zulu")
            layout.value = Layout(order = listOf("p.3", "p.1", "p.2"), hidden = setOf("p.1", "p.2", "p.3"))

            val state = viewModel(listOf(beta, alpha, zulu)).uiState.value

            assertEquals(listOf(alpha, beta, zulu), state.hidden)
        }

    @Test
    fun `unhides an app and saves the layout`() =
        runTest {
            val viewModel = viewModel()

            viewModel.unhide(b)

            assertEquals(listOf(Layout(order = listOf("a", "b"), hidden = emptySet())), saved)
            assertTrue(
                viewModel.uiState.value.hidden
                    .isEmpty(),
            )
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
    fun `is not loaded before the layout is known`() =
        runTest {
            val viewModel = SettingsViewModel(emptyFlow(), appStyle, {}, {})
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertFalse(viewModel.uiState.value.loaded)
        }
}
