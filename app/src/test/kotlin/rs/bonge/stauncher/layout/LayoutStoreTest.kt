package rs.bonge.stauncher.layout

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
import java.io.File

class LayoutStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file get() = File(folder.root, "layout.json")

    // DataStore allows one active store per file, so a store is closed before the file is opened again.
    private suspend fun <T> withRepository(block: suspend (LayoutRepository) -> T): T {
        val job = SupervisorJob()
        try {
            return block(LayoutRepository(createLayoutStore(file, CoroutineScope(job + Dispatchers.IO))))
        } finally {
            (job as Job).cancelAndJoin()
        }
    }

    @Test
    fun `starts with the default layout`() =
        runBlocking {
            withRepository { assertEquals(Layout(), it.layout.first()) }
        }

    @Test
    fun `reads back a saved layout from the file`() =
        runBlocking {
            val layout = Layout(order = listOf("a", "b"), hidden = setOf("b"))
            withRepository { it.save(layout) }

            // A second store has nothing in memory, so this proves the layout was written to the file.
            withRepository { assertEquals(layout, it.layout.first()) }
        }

    @Test
    fun `falls back to the default layout when the file cannot be read`() =
        runBlocking {
            file.writeText("not json")

            withRepository {
                assertEquals(Layout(), it.layout.first())
                // The unreadable file was replaced, so saving works again.
                it.save(Layout(order = listOf("a")))
            }
            withRepository { assertEquals(Layout(order = listOf("a")), it.layout.first()) }
        }

    @Test
    fun `ignores fields it does not know`() =
        runBlocking {
            file.writeText("""{"order":["a"],"columns":3}""")

            withRepository { assertEquals(Layout(order = listOf("a")), it.layout.first()) }
        }
}
