package rs.bonge.stauncher.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import rs.bonge.stauncher.MainDispatcherRule
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.apps.testApp

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun HomeViewModel.packages() = uiState.value.apps.map { it.packageName }

    @Test
    fun `shows apps oldest install first`() =
        runTest {
            val upstream =
                MutableStateFlow(
                    listOf(testApp("b", firstInstallTime = 2), testApp("a", firstInstallTime = 1)),
                )
            val viewModel = HomeViewModel(upstream)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertEquals(listOf("a", "b"), viewModel.packages())
        }

    @Test
    fun `puts an app installed later at the end`() =
        runTest {
            val upstream = MutableStateFlow(listOf(testApp("a", firstInstallTime = 1)))
            val viewModel = HomeViewModel(upstream)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            upstream.value = listOf(testApp("a", firstInstallTime = 1), testApp("z", firstInstallTime = 5))

            assertEquals(listOf("a", "z"), viewModel.packages())
        }

    @Test
    fun `drops an uninstalled app`() =
        runTest {
            val upstream = MutableStateFlow(listOf(testApp("a"), testApp("b")))
            val viewModel = HomeViewModel(upstream)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            upstream.value = listOf(testApp("b"))

            assertEquals(listOf("b"), viewModel.packages())
        }

    @Test
    fun `observes the apps only while the UI collects the state`() =
        runTest {
            val upstream = MutableSharedFlow<List<InstalledApp>>()
            val viewModel = HomeViewModel(upstream)
            assertEquals(0, upstream.subscriptionCount.value)

            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            assertEquals(1, upstream.subscriptionCount.value)

            collector.cancel()
            runCurrent()
            assertEquals(0, upstream.subscriptionCount.value)
        }
}
