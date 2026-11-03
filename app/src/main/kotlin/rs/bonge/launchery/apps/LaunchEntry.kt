package rs.bonge.launchery.apps

internal data class LaunchEntry(
    val packageName: String,
    val className: String,
    val leanback: Boolean,
)

/** Keeps one entry per package, preferring its leanback activity, and leaves out Launchery itself. */
internal fun pickLaunchEntries(
    candidates: List<LaunchEntry>,
    selfPackage: String,
): List<LaunchEntry> =
    candidates
        .filter { it.packageName != selfPackage }
        .sortedByDescending { it.leanback }
        .distinctBy { it.packageName }
