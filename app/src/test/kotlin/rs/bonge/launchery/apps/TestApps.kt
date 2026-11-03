package rs.bonge.launchery.apps

fun testApp(
    packageName: String,
    label: String = packageName,
    firstInstallTime: Long = 0,
    system: Boolean = false,
) = InstalledApp(
    packageName = packageName,
    className = "$packageName.Main",
    label = label,
    leanback = true,
    hasBanner = true,
    system = system,
    firstInstallTime = firstInstallTime,
    lastUpdateTime = firstInstallTime,
)
