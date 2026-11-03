package rs.bonge.launchery.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import rs.bonge.launchery.storage.createJsonStore
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file get() = File(folder.root, "settings.json")

    // DataStore allows one active store per file, so a store is closed before the file is opened again.
    private suspend fun <T> withRepository(block: suspend (SettingsRepository) -> T): T {
        val job = SupervisorJob()
        try {
            val store =
                createJsonStore(file, CoroutineScope(job + Dispatchers.IO), UserSettings.serializer()) {
                    UserSettings()
                }
            return block(SettingsRepository(store))
        } finally {
            (job as Job).cancelAndJoin()
        }
    }

    @Test
    fun `starts with round icons`() =
        runBlocking {
            withRepository { assertEquals(AppStyle.RoundIcons, it.appStyle.first()) }
        }

    @Test
    fun `keeps the style the user switched to`() =
        runBlocking {
            withRepository { it.toggleAppStyle() }

            // A second store has nothing in memory, so this proves the style was written to the file.
            withRepository { assertEquals(AppStyle.Banners, it.appStyle.first()) }
        }

    @Test
    fun `switches back and forth`() =
        runBlocking {
            withRepository {
                it.toggleAppStyle()
                it.toggleAppStyle()

                assertEquals(AppStyle.RoundIcons, it.appStyle.first())
            }
        }

    @Test
    fun `falls back to round icons when the file cannot be read`() =
        runBlocking {
            file.writeText("not json")

            withRepository { assertEquals(AppStyle.RoundIcons, it.appStyle.first()) }
        }

    @Test
    fun `falls back to round icons for a style it does not know`() =
        runBlocking {
            file.writeText("""{"appStyle":"Tiles"}""")

            withRepository { assertEquals(AppStyle.RoundIcons, it.appStyle.first()) }
        }
}
