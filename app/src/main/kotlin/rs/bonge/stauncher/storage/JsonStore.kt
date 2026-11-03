package rs.bonge.stauncher.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
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

private class JsonSerializer<T>(
    private val serializer: KSerializer<T>,
    private val default: () -> T,
) : Serializer<T> {
    override val defaultValue: T get() = default()

    override suspend fun readFrom(input: InputStream): T =
        try {
            json.decodeFromString(serializer, input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read the saved ${serializer.descriptor.serialName}", e)
        }

    override suspend fun writeTo(
        t: T,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}

/**
 * A store that keeps one [serializer]-encoded value as JSON in [file]. A file that does not exist yet, or cannot be
 * read, gives [default]. That is a function so that it runs only when it is needed, on the store's own thread.
 */
fun <T> createJsonStore(
    file: File,
    scope: CoroutineScope,
    serializer: KSerializer<T>,
    default: () -> T,
): DataStore<T> =
    DataStoreFactory.create(
        serializer = JsonSerializer(serializer, default),
        corruptionHandler = ReplaceFileCorruptionHandler { default() },
        scope = scope,
        produceFile = { file },
    )
