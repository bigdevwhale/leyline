package app.leyline

import app.leyline.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistilTest {
    /** The sky's ingredients are distilled from the one there is most of, never from themselves. */
    @Test fun fromThePlentiest() {
        val bag = mapOf("moon" to 18, "wind" to 5, "frost" to 30)
        assertEquals("moon", Book.distilFrom(bag, "frost"))
        assertEquals("frost", Book.distilFrom(bag, "storm"))
    }

    @Test fun needsEnoughOfOne() {
        assertNull(Book.distilFrom(mapOf("moon" to Book.DISTIL - 1, "wind" to 2), "storm"))
        assertEquals("moon", Book.distilFrom(mapOf("moon" to Book.DISTIL), "storm"))
    }

    /** Only what the weather gives can be distilled: the road's ingredients are found on the road. */
    @Test fun onlyTheSkys() {
        assertNull(Book.distilFrom(mapOf("moon" to 50), "wax"))
    }
}
