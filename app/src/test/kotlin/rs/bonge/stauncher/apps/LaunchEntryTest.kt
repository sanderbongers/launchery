package rs.bonge.stauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchEntryTest {
    @Test
    fun `prefers the leanback activity of a package that has both`() {
        val candidates =
            listOf(
                LaunchEntry("tv.app", "tv.app.PhoneActivity", leanback = false),
                LaunchEntry("tv.app", "tv.app.TvActivity", leanback = true),
            )

        val picked = pickLaunchEntries(candidates, selfPackage = "rs.bonge.stauncher")

        assertEquals(listOf(LaunchEntry("tv.app", "tv.app.TvActivity", leanback = true)), picked)
    }

    @Test
    fun `keeps a package that only has a launcher activity`() {
        val candidates = listOf(LaunchEntry("phone.app", "phone.app.Main", leanback = false))

        val picked = pickLaunchEntries(candidates, selfPackage = "rs.bonge.stauncher")

        assertEquals(candidates, picked)
    }

    @Test
    fun `keeps one entry per package when it has several activities of the same kind`() {
        val candidates =
            listOf(
                LaunchEntry("tv.app", "tv.app.First", leanback = true),
                LaunchEntry("tv.app", "tv.app.Second", leanback = true),
            )

        val picked = pickLaunchEntries(candidates, selfPackage = "rs.bonge.stauncher")

        assertEquals(listOf(LaunchEntry("tv.app", "tv.app.First", leanback = true)), picked)
    }

    @Test
    fun `leaves out Stauncher itself`() {
        val candidates =
            listOf(
                LaunchEntry("rs.bonge.stauncher", "rs.bonge.stauncher.HomeActivity", leanback = true),
                LaunchEntry("tv.app", "tv.app.Main", leanback = true),
            )

        val picked = pickLaunchEntries(candidates, selfPackage = "rs.bonge.stauncher")

        assertEquals(listOf("tv.app"), picked.map { it.packageName })
    }
}
