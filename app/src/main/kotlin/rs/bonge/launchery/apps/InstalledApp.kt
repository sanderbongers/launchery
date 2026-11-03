package rs.bonge.launchery.apps

/** An app on the home row, identified by the one activity Launchery shows and starts for its package. */
data class InstalledApp(
    val packageName: String,
    val className: String,
    val label: String,
    val leanback: Boolean,
    val hasBanner: Boolean,
    val system: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
)

fun List<InstalledApp>.sortedByInstallDate(): List<InstalledApp> =
    sortedWith(
        compareBy<InstalledApp> { it.firstInstallTime }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            .thenBy { it.packageName },
    )
