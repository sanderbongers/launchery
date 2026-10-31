package rs.bonge.stauncher.layout

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow

class LayoutRepository(
    private val store: DataStore<Layout>,
) {
    val layout: Flow<Layout> = store.data

    suspend fun save(layout: Layout) {
        store.updateData { layout }
    }
}
