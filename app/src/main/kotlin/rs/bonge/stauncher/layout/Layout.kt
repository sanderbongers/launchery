package rs.bonge.stauncher.layout

import kotlinx.serialization.Serializable

/**
 * What the user arranged. [order] lists package names, hidden apps included, so that an unhidden app returns to its
 * slot. Packages that are not installed any more are ignored when the layout is applied and dropped at the next save.
 */
@Serializable
data class Layout(
    val order: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
)
