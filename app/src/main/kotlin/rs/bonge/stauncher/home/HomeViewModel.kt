package rs.bonge.stauncher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rs.bonge.stauncher.StauncherApplication
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.apps.sortedByInstallDate

class HomeViewModel(
    apps: Flow<List<InstalledApp>>,
) : ViewModel() {
    // No stop timeout: once the UI stops collecting, the upstream stops too, which unregisters the package callback.
    val uiState: StateFlow<HomeUiState> =
        apps
            .map { HomeUiState(it.sortedByInstallDate()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), HomeUiState())

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    val container = (this[APPLICATION_KEY] as StauncherApplication).container
                    HomeViewModel(container.appRepository.apps)
                }
            }
    }
}
