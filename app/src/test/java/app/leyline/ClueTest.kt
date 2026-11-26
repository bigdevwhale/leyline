package app.leyline

import app.leyline.data.State
import app.leyline.data.json
import app.leyline.data.stateOf
import app.leyline.domain.Book
import app.leyline.domain.Cat
import app.leyline.domain.Clues
import app.leyline.domain.Lab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class ClueTest {
    /** Clues keep coming until every page lacks only its last ingredient – that one is for the cauldron. */
    @Test fun neverTheWholePage() {
        var s = State()
        var n = 0
        while (true) {
            val c = Clues.next(s) ?: break
            assertTrue(c !in s.clues)
            s = s.copy(clues = s.clues + c)
            n++
        }
        val pages = Book.recipes.filter { it.id != "great" && it.cat != Cat.GIFT }
        assertEquals(pages.sumOf { it.needs.size - 1 }, n)
        pages.forEach { r -> assertEquals(r.id, r.needs.size - 1, Clues.of(s.clues, r).size) }
        assertNull(Clues.next(s))
    }

    @Test fun foundPagesGetNoClues() {
        val all = Book.recipes.map { it.id }.toSet()
        assertNull(Clues.next(State(known = all)))
        val c = Clues.next(State(known = all - "tailwind"))
        assertNotNull(c)
        assertTrue(c!!.startsWith("tailwind:"))
    }

    @Test fun giveSaysIt() {
        var said = ""
        val s = Clues.give(State()) { said = it }
        assertEquals(1, s.clues.size)
        assertTrue(said.isNotEmpty())
    }

    /** An old dud's hint is counted against what's still unfound: once its page is found, it cools. */
    @Test fun hintFollowsTheGrimoire() {
        val set = listOf("wind", "wind2").take(1) + "feather" // tailwind is wind + feather
        assertEquals(3, Lab.warmth(set + "stone", emptySet()))
        val rest = Book.recipes.filter { r -> r.id != "great" && (setOf("wind", "feather", "stone") intersect r.needs.keys).size >= 2 }.map { it.id }.toSet()
        assertTrue(Lab.warmth(set + "stone", rest) < 3)
        assertEquals(listOf("feather", "stone", "wind"), Lab.parts(Lab.key(set + "stone")))
    }

    @Test fun cluesSurviveJson() {
        val s = State(clues = setOf("tailwind:wind", "aurora:frost"))
        assertEquals(s.clues, stateOf(s.json()).clues)
    }
}
