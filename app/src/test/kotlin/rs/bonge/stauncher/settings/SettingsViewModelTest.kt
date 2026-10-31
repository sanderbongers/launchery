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
    private val saved = mutableListOf<Layout>()

    private fun TestScope.viewModel(): SettingsViewModel {
        val arranged = combine(MutableStateFlow(listOf(a, b)), layout, ::arrangeApps)
        val viewModel =
            SettingsViewModel(arranged) {
                saved += it
                layout.value = it
            }
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
    fun `is not loaded before the layout is known`() =
        runTest {
            val viewModel = SettingsViewModel(emptyFlow()) {}
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertFalse(viewModel.uiState.value.loaded)
        }
}
