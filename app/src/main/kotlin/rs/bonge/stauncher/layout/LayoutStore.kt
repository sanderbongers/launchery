package rs.bonge.stauncher.layout

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private val json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

internal class LayoutSerializer(
    private val defaultLayout: () -> Layout,
) : Serializer<Layout> {
    override val defaultValue: Layout get() = defaultLayout()

    override suspend fun readFrom(input: InputStream): Layout =
        try {
            json.decodeFromString(Layout.serializer(), input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read the saved layout", e)
        }

    override suspend fun writeTo(
        t: Layout,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(Layout.serializer(), t).encodeToByteArray())
    }
}

/**
 * A file that does not exist yet, or cannot be read, gives [defaultLayout]. By default that orders apps by install date.
 * It is a function so that it runs only when it is needed, on the store's own thread.
 */
fun createLayoutStore(
    file: File,
    scope: CoroutineScope,
    defaultLayout: () -> Layout = { Layout() },
): DataStore<Layout> =
    DataStoreFactory.create(
        serializer = LayoutSerializer(defaultLayout),
        corruptionHandler = ReplaceFileCorruptionHandler { defaultLayout() },
        scope = scope,
        produceFile = { file },
    )
