package rs.bonge.stauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp

private val RowPadding = 48.dp
private val CardSpacing = 16.dp

// Room for the focused card's scale and border, which the row would otherwise clip.
private val RowVerticalPadding = 16.dp

/** Which app had focus last. Plain fields, so that reading them in composition does not subscribe to them. */
private class FocusTracker {
    var index = 0
    var packageName: String? = null
}

/**
 * The home row. Left from the first app and right from the last wrap around, on a fresh key press only. When the
 * focused app disappears, focus moves to the app that takes its index.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    apps: List<InstalledApp>,
    bannerLoader: BannerLoader,
    onLaunch: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pivotPx = with(LocalDensity.current) { RowPadding.toPx() }
    val tracker = remember { FocusTracker() }
    var pendingFocus by remember { mutableStateOf<Int?>(null) }

    // Compose moves focus to another card by itself when the focused card leaves, which would overwrite the tracker
    // before an effect runs. So decide here, in composition, whether the focused app is gone. This also covers the
    // first load, when nothing is focused yet.
    val focusedAppGone = apps.isNotEmpty() && apps.none { it.packageName == tracker.packageName }
    val refocusIndex = tracker.index.coerceAtMost(apps.lastIndex)
    LaunchedEffect(apps) {
        if (focusedAppGone) pendingFocus = refocusIndex
    }
    // An off-screen target has to be composed before it can take focus.
    LaunchedEffect(pendingFocus) {
        pendingFocus?.let { listState.scrollToItem(it) }
    }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides remember(pivotPx) { StartPivotBringIntoViewSpec(pivotPx) },
    ) {
        LazyRow(
            state = listState,
            modifier =
                modifier.onPreviewKeyEvent { event ->
                    val target = wrapTarget(event, tracker.index, apps.lastIndex)
                    target?.let { pendingFocus = it }
                    target != null
                },
            contentPadding = PaddingValues(horizontal = RowPadding, vertical = RowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(CardSpacing),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                val focusRequester = remember { FocusRequester() }
                if (pendingFocus == index) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        pendingFocus = null
                    }
                }
                AppCard(
                    app = app,
                    bannerLoader = bannerLoader,
                    onClick = { onLaunch(app) },
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    tracker.index = index
                                    tracker.packageName = app.packageName
                                }
                            },
                )
            }
        }
    }
}

/**
 * The index to wrap focus to, or null to leave the key press to normal focus movement. Only a fresh press wraps: the
 * repeats of a held key stop at the end of the row.
 */
private fun wrapTarget(
    event: KeyEvent,
    focusedIndex: Int,
    lastIndex: Int,
): Int? {
    val freshPress = event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0
    if (!freshPress || lastIndex < 1) return null
    return when {
        event.key == Key.DirectionLeft && focusedIndex == 0 -> lastIndex
        event.key == Key.DirectionRight && focusedIndex == lastIndex -> 0
        else -> null
    }
}
