package rs.bonge.stauncher.apps

fun testApp(
    packageName: String,
    label: String = packageName,
    firstInstallTime: Long = 0,
) = InstalledApp(
    packageName = packageName,
    className = "$packageName.Main",
    label = label,
    leanback = true,
    hasBanner = true,
    firstInstallTime = firstInstallTime,
    lastUpdateTime = firstInstallTime,
)
