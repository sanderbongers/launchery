package rs.bonge.stauncher.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class ClockTest {
    // Records the receivers instead of passing them on, so that the system never delivers anything to them.
    private class RecordingContext(
        base: Context,
    ) : ContextWrapper(base) {
        val receivers = CopyOnWriteArrayList<Pair<BroadcastReceiver, IntentFilter>>()

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            flags: Int,
        ): Intent? {
            receivers += receiver!! to filter!!
            return null
        }

        override fun unregisterReceiver(receiver: BroadcastReceiver?) {
            receivers.removeAll { it.first === receiver }
        }

        fun deliver(action: String) = receivers.forEach { (receiver, _) -> receiver.onReceive(this, Intent(action)) }
    }

    private val context = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun `gives the time at once and again each time the system reports a change`() =
        runBlocking {
            var count = 0
            val clock = Clock(context) { "time ${count++}" }
            val times = Channel<String>(Channel.UNLIMITED)
            val collector = launch(Dispatchers.Default) { clock.time.collect { times.send(it) } }

            assertEquals("time 0", withTimeout(2_000) { times.receive() })
            context.deliver(Intent.ACTION_TIME_TICK)
            assertEquals("time 1", withTimeout(2_000) { times.receive() })
            collector.cancelAndJoin()
        }

    @Test
    fun `listens for the tick and for time and zone changes only while it is collected`() =
        runBlocking {
            val clock = Clock(context) { "time" }
            assertTrue(context.receivers.isEmpty())

            val times = Channel<String>(Channel.UNLIMITED)
            val collector = launch(Dispatchers.Default) { clock.time.collect { times.send(it) } }
            withTimeout(2_000) { times.receive() }

            val filter = context.receivers.single().second
            assertTrue(filter.hasAction(Intent.ACTION_TIME_TICK))
            assertTrue(filter.hasAction(Intent.ACTION_TIME_CHANGED))
            assertTrue(filter.hasAction(Intent.ACTION_TIMEZONE_CHANGED))
            collector.cancelAndJoin()
            assertTrue(context.receivers.isEmpty())
        }

    @Test
    fun `stays quiet when a report leaves the time as it was`() =
        runBlocking {
            val clock = Clock(context) { "the same" }
            val times = Channel<String>(Channel.UNLIMITED)
            val collector = launch(Dispatchers.Default) { clock.time.collect { times.send(it) } }
            withTimeout(2_000) { times.receive() }

            context.deliver(Intent.ACTION_TIME_CHANGED)
            context.deliver(Intent.ACTION_TIME_TICK)

            collector.cancelAndJoin()
            assertTrue(times.tryReceive().isFailure)
        }
}
