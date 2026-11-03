package rs.bonge.stauncher.home

import androidx.test.platform.app.InstrumentationRegistry
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp

/** App [number] is `test.app<number>`, named "App <number>". The package does not exist, so no art loads for it. */
fun testApp(
    number: Int,
    system: Boolean = false,
) = InstalledApp(
    packageName = "test.app$number",
    className = "test.app$number.Main",
    label = "App $number",
    leanback = true,
    hasBanner = false,
    system = system,
    firstInstallTime = number.toLong(),
    lastUpdateTime = number.toLong(),
)

fun testApps(count: Int) = (1..count).map { testApp(it) }

fun testBannerLoader() = BannerLoader(InstrumentationRegistry.getInstrumentation().targetContext.packageManager)
