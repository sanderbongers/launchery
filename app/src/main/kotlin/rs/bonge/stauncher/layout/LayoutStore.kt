package rs.bonge.stauncher.layout

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import rs.bonge.stauncher.storage.createJsonStore
import java.io.File

/**
 * A file that does not exist yet, or cannot be read, gives [defaultLayout]. By default that orders apps by install date.
 * It is a function so that it runs only when it is needed, on the store's own thread.
 */
fun createLayoutStore(
    file: File,
    scope: CoroutineScope,
    defaultLayout: () -> Layout = { Layout() },
): DataStore<Layout> = createJsonStore(file, scope, Layout.serializer(), defaultLayout)
