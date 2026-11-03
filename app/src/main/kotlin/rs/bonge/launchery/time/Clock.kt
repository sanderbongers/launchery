package rs.bonge.launchery.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Date

/**
 * The time of day, formatted the way the system settings ask for (12 or 24 hours). [time] holds one receiver, but only
 * while something collects it. Collected from a started UI, nothing runs in the background. The tick is registered in
 * code because the system does not deliver it to a receiver declared in the manifest.
 */
class Clock(
    private val context: Context,
    private val format: () -> String = { DateFormat.getTimeFormat(context).format(Date()) },
) {
    fun now(): String = format()

    val time: Flow<String> =
        callbackFlow {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        trySend(now())
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                }
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            trySend(now())
            awaitClose { context.unregisterReceiver(receiver) }
        }.distinctUntilChanged()
}
