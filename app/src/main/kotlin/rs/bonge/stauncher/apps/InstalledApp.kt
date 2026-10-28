package rs.bonge.stauncher.apps

/** An app on the home row, identified by the one activity Stauncher shows and starts for its package. */
data class InstalledApp(
    val packageName: String,
    val className: String,
    val label: String,
    val leanback: Boolean,
    val hasBanner: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
)

fun List<InstalledApp>.sortedByInstallDate(): List<InstalledApp> =
    sortedWith(
        compareBy<InstalledApp> { it.firstInstallTime }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            .thenBy { it.packageName },
    )
