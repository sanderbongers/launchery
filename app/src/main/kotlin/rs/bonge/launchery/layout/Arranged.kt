package rs.bonge.launchery.layout

import rs.bonge.launchery.apps.InstalledApp
import rs.bonge.launchery.apps.sortedByInstallDate
import java.text.Collator

// The hidden apps are listed to be found by name, not by the slot they came from.
private val byName = compareBy<InstalledApp, String>(Collator.getInstance()) { it.label }.thenBy { it.packageName }

/** The installed apps as the home row and the settings sheet show them. */
class Arranged internal constructor(
    private val all: List<InstalledApp>,
    private val hiddenPackages: Set<String>,
) {
    val visible: List<InstalledApp> = all.filter { it.packageName !in hiddenPackages }
    val hidden: List<InstalledApp> = all.filter { it.packageName in hiddenPackages }.sortedWith(byName)

    /**
     * The layout to save once the visible apps are in [visibleOrder]. Hidden apps keep their slots, so the visible
     * apps are rearranged among the slots they already occupy.
     */
    fun layoutAfterReordering(visibleOrder: List<String>): Layout {
        val current = visible.map { it.packageName }
        val requested = visibleOrder.filter { it in current }.distinct()
        val slots = ArrayDeque(requested + current.filter { it !in requested })
        val order = all.map { if (it.packageName in hiddenPackages) it.packageName else slots.removeFirst() }
        return Layout(order, hiddenPackages)
    }

    fun layoutAfterHiding(packageName: String): Layout = layout(hiddenPackages + packageName)

    fun layoutAfterUnhiding(packageName: String): Layout = layout(hiddenPackages - packageName)

    private fun layout(hidden: Set<String>) = Layout(all.map { it.packageName }, hidden)
}

/**
 * Applies a saved layout to the installed apps. Saved apps keep their saved order; apps the layout does not know yet
 * come after them, oldest install first.
 */
fun arrangeApps(
    installed: List<InstalledApp>,
    layout: Layout,
): Arranged {
    val byPackage = installed.associateBy { it.packageName }
    val saved = layout.order.distinct().mapNotNull { byPackage[it] }
    val savedPackages = saved.mapTo(mutableSetOf()) { it.packageName }
    val added = installed.filter { it.packageName !in savedPackages }.sortedByInstallDate()
    return Arranged(saved + added, layout.hidden.filterTo(mutableSetOf()) { it in byPackage })
}
