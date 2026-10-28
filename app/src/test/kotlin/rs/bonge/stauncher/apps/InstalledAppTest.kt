package rs.bonge.stauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppTest {
    @Test
    fun `orders by install date, oldest first`() {
        val apps =
            listOf(
                testApp("c", firstInstallTime = 30),
                testApp("a", firstInstallTime = 10),
                testApp("b", firstInstallTime = 20),
            )

        assertEquals(listOf("a", "b", "c"), apps.sortedByInstallDate().map { it.packageName })
    }

    @Test
    fun `orders apps with the same install date by label, ignoring case`() {
        val apps =
            listOf(
                testApp("p1", label = "settings"),
                testApp("p2", label = "Netflix"),
                testApp("p3", label = "Apps"),
            )

        assertEquals(listOf("Apps", "Netflix", "settings"), apps.sortedByInstallDate().map { it.label })
    }

    @Test
    fun `orders apps with the same install date and label by package name`() {
        val apps = listOf(testApp("b", label = "Same"), testApp("a", label = "Same"))

        assertEquals(listOf("a", "b"), apps.sortedByInstallDate().map { it.packageName })
    }
}
