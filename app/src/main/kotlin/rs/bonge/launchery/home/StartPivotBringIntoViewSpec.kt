package rs.bonge.launchery.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * Scrolls so that the focused item's leading edge lands on [pivotPx], which holds the focused card in a fixed slot
 * while the row slides underneath. A scroll position that cannot reach the pivot, such as the end of the row, is
 * clamped by the list itself.
 */
@OptIn(ExperimentalFoundationApi::class)
class StartPivotBringIntoViewSpec(
    private val pivotPx: Float,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = offset - pivotPx
}
