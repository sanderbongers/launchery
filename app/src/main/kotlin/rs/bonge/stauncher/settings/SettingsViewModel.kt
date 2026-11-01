package rs.bonge.stauncher.settings

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
import kotlinx.coroutines.launch
import rs.bonge.stauncher.StauncherApplication
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.layout.Arranged
import rs.bonge.stauncher.layout.Layout
import java.io.IOException
import java.text.Collator

// The hidden apps are listed to be found by name, not by the slot they came from.
private val byName = compareBy<InstalledApp, String>(Collator.getInstance()) { it.label }.thenBy { it.packageName }

/** [loaded] tells an empty list from one that has not been read yet. */
data class SettingsUiState(
    val hidden: List<InstalledApp> = emptyList(),
    val loaded: Boolean = false,
)

class SettingsViewModel(
    arranged: Flow<Arranged>,
    private val saveLayout: suspend (Layout) -> Unit,
) : ViewModel() {
    private val latest: StateFlow<Arranged?> = arranged.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val uiState: StateFlow<SettingsUiState> =
        latest
            .map { SettingsUiState(it?.hidden.orEmpty().sortedWith(byName), loaded = it != null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SettingsUiState())

    fun unhide(app: InstalledApp) {
        val arranged = latest.value ?: return
        viewModelScope.launch {
            try {
                saveLayout(arranged.layoutAfterUnhiding(app.packageName))
            } catch (_: IOException) {
                // The layout stays as it was.
            }
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    val container = (this[APPLICATION_KEY] as StauncherApplication).container
                    SettingsViewModel(container.arrangedApps, container.layoutRepository::save)
                }
            }
    }
}
