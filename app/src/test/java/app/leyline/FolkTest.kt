package app.leyline

import app.leyline.data.Errand
import app.leyline.data.Quarter
import app.leyline.data.State
import app.leyline.data.json
import app.leyline.data.stateOf
import app.leyline.domain.Book
import app.leyline.domain.Cat
import app.leyline.domain.Folk
import app.leyline.domain.Kind
import app.leyline.domain.Pages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class FolkTest {
    private val home = Quarter("1", "Сады", "Gardens", 55.75, 37.60, Kind.GARDEN, Kind.HOMES, visits = 5, firstDay = 1)
    private val far = Quarter("2", "Кузни", "Forges", 55.79, 37.67, Kind.FORGE, Kind.GATE, visits = 1, firstDay = 2)
    private val near = Quarter("3", "Торжище", "Market", 55.751, 37.601, Kind.BAZAAR, Kind.OLD, visits = 3, firstDay = 3, changes = 1)
    private val s = State(quarters = listOf(home, far, near).associateBy { it.id }, here = "1")

    private fun who(f: (Folk.Resident) -> Boolean) = Folk.all.indexOfFirst(f)

    @Test fun residentsSettleInOftenVisitedQuarters() {
        val t = Folk.settle(s)
        assertEquals(setOf("1", "3"), t.folk.map { it.quarter }.toSet())
        assertEquals(2, t.folk.map { it.who }.distinct().size)
        // settling again adds nobody
        assertEquals(t.folk, Folk.settle(t).folk)
    }

    @Test fun letterGoesFarAndCountsFromAnywhere() {
        val e = Errand("1", who { it.steps[0].ask == Folk.Ask.Letter }, since = 1)
        assertEquals("2", Folk.pickTarget(s, e))
        val sent = e.copy(target = "2")
        assertFalse(Folk.ready(s.copy(folk = listOf(sent)), sent))
        // riding into the letter's quarter delivers it; reporting works from anywhere
        val arrived = Folk.entered(s.copy(folk = listOf(sent)), "2").single()
        assertTrue(arrived.arrived)
        assertTrue(Folk.ready(s.copy(folk = listOf(arrived), here = "3"), arrived))
    }

    /** Before a letter is taken its words name the quarter it will go to, not the last letter's one. */
    @Test fun untakenLetterNamesItsOwnQuarter() {
        val i = who { it.ru == "Кузьма" }
        val e = Errand("1", i, step = 2, target = "3")
        assertEquals("2", Folk.pickTarget(s, e))
        assertTrue(Folk.text(s, e).contains(far.name))
    }

    @Test fun ingredientsOnlyInTheResidentsQuarter() {
        val e = Errand("1", who { (it.steps[0].ask as? Folk.Ask.Give)?.ing == "copper" }, since = 1)
        val rich = s.copy(bag = mapOf("copper" to 3), folk = listOf(e))
        assertTrue(Folk.ready(rich, e))
        assertFalse(Folk.ready(rich.copy(here = "2"), e))
        assertFalse(Folk.ready(rich.copy(bag = mapOf("copper" to 2)), e))
    }

    @Test fun farRideCountsOnlyFarAway() {
        val e = Errand("1", who { it.steps[1].ask is Folk.Ask.Far }, step = 1, since = 1)
        val st = s.copy(folk = listOf(e))
        assertFalse(Folk.entered(st, "3").single().arrived)
        val back = Folk.entered(st, "2").single()
        assertTrue(back.arrived)
        assertTrue(Folk.ready(st.copy(folk = listOf(back)), back))
        assertFalse(Folk.ready(st.copy(folk = listOf(back), here = "2"), back))
    }

    @Test fun potionFromTheStock() {
        val i = who { it.ru == "Степан" }
        val e = Errand("1", i, step = 2, since = 1)
        assertFalse(Folk.ready(s.copy(folk = listOf(e)), e))
        assertTrue(Folk.ready(s.copy(folk = listOf(e), stock = mapOf("seeker" to 1)), e))
    }

    @Test fun everyResidentAsksForRealThings() {
        assertEquals(12, Folk.all.size)
        Folk.all.forEach { r ->
            assertEquals(3, r.steps.size)
            r.steps.forEach { st ->
                when (val a = st.ask) {
                    is Folk.Ask.Give -> assertTrue(a.ing, a.ing in Book.ing)
                    is Folk.Ask.Potion -> assertEquals(Cat.ELIXIR, Book.recipe(a.recipe).cat)
                    Folk.Ask.Letter -> assertTrue(r.en, st.ru.contains("{q}") && st.en.contains("{q}"))
                    else -> {}
                }
                assertFalse(st.ru, st.ru.contains("—"))
            }
        }
    }

    @Test fun folkAndChangesSurviveJson() {
        val t = Folk.settle(s).let { it.copy(folk = it.folk.map { e -> e.copy(since = 5, target = "2", arrived = true, step = 1) }) }
        val back = stateOf(t.json())
        assertEquals(t.folk, back.folk)
        assertEquals(1, back.quarters["3"]!!.changes)
    }

    @Test fun fullPagesGiveOnceAndAllGivesTheTitle() {
        val elixirs = Book.recipes.filter { it.cat == Cat.ELIXIR }.map { it.id }.toSet()
        val a = Pages.grant(s.copy(known = elixirs))
        assertEquals(Pages.SET_POWER, a.power)
        assertTrue("set:elixir" in a.told)
        // once only
        assertEquals(a, Pages.grant(a))
        val all = Pages.grant(a.copy(known = Book.recipes.map { it.id }.toSet()))
        assertEquals(Pages.SET_POWER * Pages.CATS.size + Pages.ALL_POWER, all.power)
        assertTrue("set:all" in all.told)
    }
}
