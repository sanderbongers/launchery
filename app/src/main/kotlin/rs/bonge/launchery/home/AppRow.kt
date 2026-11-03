package rs.bonge.launchery.home

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.flow.collectLatest
import rs.bonge.launchery.apps.BannerLoader
import rs.bonge.launchery.apps.InstalledApp
import rs.bonge.launchery.settings.AppStyle
import rs.bonge.launchery.ui.ScreenMargin

// Space above and below the cards, which the gap under the heading counts on. It is not room against clipping: a lazy
// row already lets its items draw 30 dp beyond it across the scroll direction.
private val RowVerticalPadding = 16.dp

/** Which app had focus last. Plain fields, so that reading them in composition does not subscribe to them. */
private class FocusTracker {
    var index = 0
    var packageName: String? = null
    var actionsWereOpen = false
    var restorePackage: String? = null
}

/**
 * The home row. A left or right key that is held down on the first or the last app breaks through to the other end of
 * the row. When the focused app disappears, focus moves to the app that takes its index. While an app is being moved
 * ([mode]) the keys move it instead, and select drops it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    apps: List<InstalledApp>,
    style: AppStyle,
    mode: HomeMode,
    homeRequests: Int,
    bannerLoader: BannerLoader,
    onLaunch: (InstalledApp) -> Unit,
    onOpenActions: (InstalledApp) -> Unit,
    onMoveBy: (Int) -> Unit,
    onDrop: () -> Unit,
    modifier: Modifier = Modifier,
    glow: GlowState = rememberGlowState(),
) {
    val listState = rememberLazyListState()
    // Moving a card makes the row jump a place, and only the cards that were composed before can slide to their new
    // places. So the row is laid out this much wider on both sides of the screen, and the card that slides into view or
    // out of it is already there.
    val slideMargin = style.itemWidth() + style.itemGap()
    val pivotPx = with(LocalDensity.current) { (slideMargin + ScreenMargin).toPx() }
    val tracker = remember { FocusTracker() }
    val breakThrough = remember { BreakThrough() }
    var pendingFocus by remember { mutableStateOf<Int?>(null) }

    // Compose moves focus to another card by itself when the focused card leaves, which would overwrite the tracker
    // before an effect runs. So decide here, in composition, whether the focused app is gone. This also covers the
    // first load, when nothing is focused yet.
    val focusedAppGone = apps.isNotEmpty() && apps.none { it.packageName == tracker.packageName }
    val refocusIndex = tracker.index.coerceAtMost(apps.lastIndex)
    LaunchedEffect(apps) {
        if (focusedAppGone) pendingFocus = refocusIndex
    }
    // The actions screen takes focus while it is open; when it closes the card gets it back. The screen's app is the
    // one that had focus. It is noted here, in composition, because the focus that falls back to the first card when
    // the screen leaves would overwrite the tracker before an effect runs.
    val actionsOpen = mode is HomeMode.Actions
    (mode as? HomeMode.Actions)?.let { tracker.restorePackage = it.app.packageName }
    LaunchedEffect(actionsOpen) {
        if (tracker.actionsWereOpen && !actionsOpen && apps.isNotEmpty()) {
            pendingFocus =
                apps.indexOfFirst { it.packageName == tracker.restorePackage }.takeIf { it >= 0 } ?: refocusIndex
        }
        tracker.actionsWereOpen = actionsOpen
    }
    // Going home starts the row over. This comes after the effect above, so that its focus target wins when the Home
    // button closes the actions screen.
    val handledHomeRequests = remember { intArrayOf(homeRequests) }
    LaunchedEffect(homeRequests) {
        if (homeRequests != handledHomeRequests[0]) {
            handledHomeRequests[0] = homeRequests
            pendingFocus = 0
        }
    }
    // An off-screen target has to be composed before it can take focus.
    LaunchedEffect(pendingFocus) {
        pendingFocus?.let { listState.scrollToItem(it) }
    }
    // The glow takes the colour of the app that has focus, and fades away when focus leaves the row. The focused app is
    // read outside composition: a recomposition on every focus change would disturb the focus handling above.
    var focusedApp by remember { mutableStateOf<InstalledApp?>(null) }
    LaunchedEffect(style) {
        snapshotFlow { focusedApp }.collectLatest { app ->
            val color = app?.let { bannerLoader.glowColor(it, artKindFor(it, style)) }
            glow.fadeTo(color?.let(::Color))
        }
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
                modifier.extendBeyondScreen(slideMargin).focusRestorer().onPreviewKeyEvent { event ->
                    if (movingPackage != null) {
                        handleMoveKey(event, onMoveBy, onDrop)
                    } else {
                        breakThrough.handle(event, tracker.index, apps.lastIndex) { pendingFocus = it }
                    }
                },
            contentPadding = PaddingValues(horizontal = slideMargin + ScreenMargin, vertical = RowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(style.itemGap()),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                val focusRequester = remember { FocusRequester() }
                if (pendingFocus == index) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        // When focus comes from outside the row, its focus restorer first gives it to the app that had
                        // it last. The second request moves it on to this app.
                        focusRequester.requestFocus()
                        pendingFocus = null
                    }
                }
                AppCard(
                    app = app,
                    style = style,
                    bannerLoader = bannerLoader,
                    onClick = { onLaunch(app) },
                    onLongClick = { onOpenActions(app) },
                    lifted = app.packageName == movingPackage,
                    // Only the cards that change place slide: a card that appears or goes simply does so.
                    itemModifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    tracker.index = index
                                    tracker.packageName = app.packageName
                                    focusedApp = app
                                } else if (focusedApp?.packageName == app.packageName) {
                                    focusedApp = null
                                }
                            }.onGloballyPositioned {
                                if (focusedApp?.packageName == app.packageName) glow.center = it.boundsInRoot().center
                            },
                )
            }
        }
    }
}

/** Measures the row [margin] wider on both sides than the space it is placed in, which clips it back to the screen. */
private fun Modifier.extendBeyondScreen(margin: Dp) =
    layout { measurable, constraints ->
        val extra = margin.roundToPx()
        val placeable = measurable.measure(constraints.offset(horizontal = 2 * extra))
        layout(placeable.width - 2 * extra, placeable.height) { placeable.place(-extra, 0) }
    }

/**
 * Left and right at the ends of the row. A press that starts on the first or the last app does nothing, and held down
 * until its first repeat it jumps once to the other end, so that a stray press never loops the row. The rest of that
 * hold is ignored. A key that is held down from further along stops at the end instead, as it has not started there.
 */
private class BreakThrough {
    private var key: Key? = null
    private var jumped = false

    /** Whether the row consumed [event]; if not, the key moves focus as usual. [jumpTo] gets the index to focus. */
    fun handle(
        event: KeyEvent,
        focusedIndex: Int,
        lastIndex: Int,
        jumpTo: (Int) -> Unit,
    ): Boolean {
        val direction = event.key
        if (direction != Key.DirectionLeft && direction != Key.DirectionRight) return false
        return when (event.type) {
            KeyEventType.KeyDown -> {
                val repeat = event.nativeKeyEvent.repeatCount
                if (repeat == 0) {
                    val atEnd = lastIndex >= 1 && focusedIndex == if (direction == Key.DirectionLeft) 0 else lastIndex
                    key = direction.takeIf { atEnd }
                    jumped = false
                }
                val startedAtEnd = key == direction
                if (startedAtEnd && repeat == 1 && !jumped) {
                    jumped = true
                    jumpTo(if (direction == Key.DirectionLeft) lastIndex else 0)
                }
                startedAtEnd
            }

            KeyEventType.KeyUp -> {
                (key == direction).also { if (it) key = null }
            }

            else -> {
                false
            }
        }
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
    when {
        event.key == Key.DirectionLeft -> if (down) onMoveBy(-1)
        event.key == Key.DirectionRight -> if (down) onMoveBy(1)
        event.isSelect() -> if (down && event.nativeKeyEvent.repeatCount == 0) onDrop()
        event.key != Key.DirectionUp && event.key != Key.DirectionDown -> return false
    }
    return true
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
