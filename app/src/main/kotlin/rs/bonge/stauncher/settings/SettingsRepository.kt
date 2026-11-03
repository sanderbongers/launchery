package rs.bonge.stauncher.settings

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val store: DataStore<UserSettings>,
) {
    val appStyle: Flow<AppStyle> = store.data.map { it.appStyle }

    /** Switches to the other style. The store makes the update atomic, so two quick presses flip it twice. */
    suspend fun toggleAppStyle() {
        store.updateData { it.copy(appStyle = it.appStyle.next()) }
    }
}
