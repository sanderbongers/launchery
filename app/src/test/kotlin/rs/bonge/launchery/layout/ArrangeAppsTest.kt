package rs.bonge.launchery.layout

import org.junit.Assert.assertEquals
import org.junit.Test
import rs.bonge.launchery.apps.testApp

class ArrangeAppsTest {
    private val a = testApp("a", firstInstallTime = 1)
    private val b = testApp("b", firstInstallTime = 2)
    private val c = testApp("c", firstInstallTime = 3)
    private val d = testApp("d", firstInstallTime = 4)

    private fun Arranged.visiblePackages() = visible.map { it.packageName }

    private fun Arranged.hiddenPackages() = hidden.map { it.packageName }

    @Test
    fun `orders by install date when nothing is saved`() {
        val arranged = arrangeApps(listOf(c, a, b), Layout())

        assertEquals(listOf("a", "b", "c"), arranged.visiblePackages())
    }

    @Test
    fun `keeps the saved order`() {
        val arranged = arrangeApps(listOf(a, b, c), Layout(order = listOf("c", "a", "b")))

        assertEquals(listOf("c", "a", "b"), arranged.visiblePackages())
    }

    @Test
    fun `puts apps installed after the layout was saved at the end, oldest first`() {
        val arranged = arrangeApps(listOf(a, b, c, d), Layout(order = listOf("b", "a")))

        assertEquals(listOf("b", "a", "c", "d"), arranged.visiblePackages())
    }

    @Test
    fun `leaves hidden apps out of the visible apps and lists them separately`() {
        val arranged = arrangeApps(listOf(a, b, c), Layout(order = listOf("a", "b", "c"), hidden = setOf("b")))

        assertEquals(listOf("a", "c"), arranged.visiblePackages())
        assertEquals(listOf("b"), arranged.hiddenPackages())
    }

    @Test
    fun `ignores saved packages that are not installed`() {
        val arranged = arrangeApps(listOf(a, c), Layout(order = listOf("a", "gone", "c"), hidden = setOf("gone", "c")))

        assertEquals(listOf("a"), arranged.visiblePackages())
        assertEquals(listOf("c"), arranged.hiddenPackages())
    }

    @Test
    fun `puts an app that was uninstalled and installed again at the end`() {
        val stored = Layout(order = listOf("a", "b", "c"))
        val saved = arrangeApps(listOf(a, c), stored).layoutAfterReordering(listOf("a", "c"))

        val arranged = arrangeApps(listOf(a, b, c), saved)

        assertEquals(listOf("a", "c", "b"), arranged.visiblePackages())
    }

    @Test
    fun `saves the full order of all installed apps when an app is hidden`() {
        val layout = arrangeApps(listOf(c, a, b), Layout()).layoutAfterHiding("b")

        assertEquals(Layout(order = listOf("a", "b", "c"), hidden = setOf("b")), layout)
    }

    @Test
    fun `drops packages that are not installed from the layout it saves`() {
        val stored = Layout(order = listOf("a", "gone", "b"), hidden = setOf("gone"))

        val layout = arrangeApps(listOf(a, b), stored).layoutAfterHiding("b")

        assertEquals(Layout(order = listOf("a", "b"), hidden = setOf("b")), layout)
    }

    @Test
    fun `puts an unhidden app back in its old slot`() {
        val hidden = arrangeApps(listOf(a, b, c), Layout()).layoutAfterHiding("b")

        val layout = arrangeApps(listOf(a, b, c), hidden).layoutAfterUnhiding("b")

        assertEquals(Layout(order = listOf("a", "b", "c"), hidden = emptySet()), layout)
        assertEquals(listOf("a", "b", "c"), arrangeApps(listOf(a, b, c), layout).visiblePackages())
    }

    @Test
    fun `reorders the visible apps among their slots, so hidden apps keep theirs`() {
        val stored = Layout(order = listOf("a", "b", "c", "d"), hidden = setOf("b"))

        val layout = arrangeApps(listOf(a, b, c, d), stored).layoutAfterReordering(listOf("d", "c", "a"))

        assertEquals(Layout(order = listOf("d", "b", "c", "a"), hidden = setOf("b")), layout)
    }

    @Test
    fun `reorders safely when the requested order is stale`() {
        val arranged = arrangeApps(listOf(a, b, c), Layout())

        // "gone" was uninstalled during the move and "c" is missing from the request.
        val layout = arranged.layoutAfterReordering(listOf("b", "gone", "a"))

        assertEquals(listOf("b", "a", "c"), layout.order)
    }
}
