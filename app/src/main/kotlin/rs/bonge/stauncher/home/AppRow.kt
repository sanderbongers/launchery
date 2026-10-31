package rs.bonge.stauncher.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.focus.focusRestorer
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
    var actionsWereOpen = false
    var restorePackage: String? = null
}

/**
 * The home row. Left from the first app and right from the last wrap around, on a fresh key press only. When the
 * focused app disappears, focus moves to the app that takes its index. While an app is being moved ([mode]) the keys
 * move it instead, and select drops it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    apps: List<InstalledApp>,
    mode: HomeMode,
    bannerLoader: BannerLoader,
    onLaunch: (InstalledApp) -> Unit,
    onOpenActions: (InstalledApp) -> Unit,
    onMoveBy: (Int) -> Unit,
    onDrop: () -> Unit,
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
    // The actions panel takes focus while it is open; when it closes the card gets it back. The panel's app is the one
    // that had focus. It is noted here, in composition, because the focus that falls back to the first card when the
    // panel leaves would overwrite the tracker before an effect runs.
    val actionsOpen = mode is HomeMode.Actions
    (mode as? HomeMode.Actions)?.let { tracker.restorePackage = it.app.packageName }
    LaunchedEffect(actionsOpen) {
        if (tracker.actionsWereOpen && !actionsOpen && apps.isNotEmpty()) {
            pendingFocus =
                apps.indexOfFirst { it.packageName == tracker.restorePackage }.takeIf { it >= 0 } ?: refocusIndex
        }
        tracker.actionsWereOpen = actionsOpen
    }
    // An off-screen target has to be composed before it can take focus.
    LaunchedEffect(pendingFocus) {
        pendingFocus?.let { listState.scrollToItem(it) }
    }
    // The moved card keeps focus, but its index changes, and it may leave the screen.
    val movingPackage = (mode as? HomeMode.Moving)?.app?.packageName
    val movingIndex = apps.indexOfFirst { it.packageName == movingPackage }
    LaunchedEffect(movingIndex) {
        if (movingIndex >= 0) {
            tracker.index = movingIndex
            listState.reveal(movingIndex)
        }
    }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides remember(pivotPx) { StartPivotBringIntoViewSpec(pivotPx) },
    ) {
        LazyRow(
            state = listState,
            modifier =
                modifier.focusRestorer().onPreviewKeyEvent { event ->
                    if (movingPackage != null) {
                        handleMoveKey(event, onMoveBy, onDrop)
                    } else {
                        val target = wrapTarget(event, tracker.index, apps.lastIndex)
                        target?.let { pendingFocus = it }
                        target != null
                    }
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
                    onLongClick = { onOpenActions(app) },
                    lifted = app.packageName == movingPackage,
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

/**
 * While an app is picked up, left and right move it, select drops it and up and down do nothing. The key-up of a key
 * that was handled on key-down is consumed too, so that the card under the focus is not clicked.
 */
private fun handleMoveKey(
    event: KeyEvent,
    onMoveBy: (Int) -> Unit,
    onDrop: () -> Unit,
): Boolean {
    val down = event.type == KeyEventType.KeyDown
    return when (event.key) {
        Key.DirectionLeft -> {
            true.also { if (down) onMoveBy(-1) }
        }

        Key.DirectionRight -> {
            true.also { if (down) onMoveBy(1) }
        }

        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
            true.also { if (down && event.nativeKeyEvent.repeatCount == 0) onDrop() }
        }

        Key.DirectionUp, Key.DirectionDown -> {
            true
        }

        else -> {
            false
        }
    }
}

/** Scrolls just far enough to bring the card fully into view. */
private suspend fun LazyListState.reveal(index: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    val startEdge = info.viewportStartOffset + info.beforeContentPadding
    val endEdge = info.viewportEndOffset - info.afterContentPadding
    when {
        item == null -> animateScrollToItem(index)
        item.offset < startEdge -> animateScrollBy((item.offset - startEdge).toFloat())
        item.offset + item.size > endEdge -> animateScrollBy((item.offset + item.size - endEdge).toFloat())
    }
}
