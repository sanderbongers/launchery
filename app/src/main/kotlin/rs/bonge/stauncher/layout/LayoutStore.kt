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

internal object LayoutSerializer : Serializer<Layout> {
    override val defaultValue = Layout()

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

/** A file that cannot be read is replaced by the default layout, which orders apps by install date. */
fun createLayoutStore(
    file: File,
    scope: CoroutineScope,
): DataStore<Layout> =
    DataStoreFactory.create(
        serializer = LayoutSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { Layout() },
        scope = scope,
        produceFile = { file },
    )
