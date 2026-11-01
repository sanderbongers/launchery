package rs.bonge.stauncher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.bonge.stauncher.StauncherApplication
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.layout.Arranged
import rs.bonge.stauncher.layout.Layout
import java.io.IOException

class HomeViewModel(
    arranged: Flow<Arranged>,
    private val saveLayout: suspend (Layout) -> Unit,
) : ViewModel() {
    // What the user is in the middle of. Package names rather than apps, so it survives a rescan.
    private sealed interface Pending {
        val packageName: String

        data class Actions(
            override val packageName: String,
        ) : Pending

        data class Moving(
            override val packageName: String,
            val order: List<String>,
        ) : Pending
    }

    private val pending = MutableStateFlow<Pending?>(null)
    private val homeRequests = MutableStateFlow(0)

    // No stop timeout: once the UI stops collecting, the upstream stops too, which unregisters the package callback.
    private val latest: StateFlow<Arranged?> = arranged.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val uiState: StateFlow<HomeUiState> =
        combine(latest, pending, homeRequests, ::build)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), HomeUiState())

    fun openActions(app: InstalledApp) {
        pending.value = Pending.Actions(app.packageName)
    }

    /** Closes the actions panel, or puts a picked-up app back where it was. */
    fun dismiss() {
        pending.value = null
    }

    /** The Home button, or Stauncher started again: whatever is open is put away unsaved, and the row starts over. */
    fun goHome() {
        pending.value = null
        homeRequests.update { it + 1 }
    }

    fun startMove() {
        val actions = pending.value as? Pending.Actions ?: return
        val order = latest.value?.visible?.map { it.packageName } ?: return
        if (order.size > 1) pending.value = Pending.Moving(actions.packageName, order)
    }

    /** Moves the picked-up app one place; it stops at the ends of the row. */
    fun moveBy(step: Int) {
        val moving = pending.value as? Pending.Moving ?: return
        val from = moving.order.indexOf(moving.packageName)
        val to = (from + step).coerceIn(0, moving.order.lastIndex)
        if (from < 0 || from == to) return
        val order = moving.order.toMutableList().apply { add(to, removeAt(from)) }
        pending.value = moving.copy(order = order)
    }

    fun drop() {
        val moving = pending.value as? Pending.Moving ?: return
        val arranged = latest.value ?: return
        save(arranged.layoutAfterReordering(moving.order))
    }

    fun hide() {
        val actions = pending.value as? Pending.Actions ?: return
        val arranged = latest.value ?: return
        save(arranged.layoutAfterHiding(actions.packageName))
    }

    // The pending state stays until the write is done, so the row does not flash the old order in between.
    private fun save(layout: Layout) {
        viewModelScope.launch {
            try {
                saveLayout(layout)
            } catch (_: IOException) {
                // The layout stays as it was.
            } finally {
                pending.value = null
            }
        }
    }

    private fun build(
        arranged: Arranged?,
        pending: Pending?,
        homeRequests: Int,
    ): HomeUiState {
        if (arranged == null) return HomeUiState(homeRequests = homeRequests)
        val visible = arranged.visible
        val target = visible.firstOrNull { it.packageName == pending?.packageName }
        return when {
            target == null -> {
                HomeUiState(visible, loaded = true, homeRequests = homeRequests)
            }

            pending is Pending.Moving -> {
                HomeUiState(
                    preview(visible, pending.order),
                    true,
                    HomeMode.Moving(target),
                    homeRequests,
                )
            }

            else -> {
                HomeUiState(visible, true, HomeMode.Actions(target), homeRequests)
            }
        }
    }

    // An app that arrived during the move is not in the preview order yet, so it goes to the end.
    private fun preview(
        visible: List<InstalledApp>,
        order: List<String>,
    ): List<InstalledApp> {
        val byPackage = visible.associateBy { it.packageName }
        return order.mapNotNull { byPackage[it] } + visible.filter { it.packageName !in order }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    val container = (this[APPLICATION_KEY] as StauncherApplication).container
                    HomeViewModel(container.arrangedApps, container.layoutRepository::save)
                }
            }
    }
}
