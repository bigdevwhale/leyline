package app.leyline

import app.leyline.domain.Book
import app.leyline.domain.Decors
import app.leyline.domain.Lab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabTest {
    /** Every recipe but the great elixir must be findable in a three-slot cauldron, and no two may share a set. */
    @Test fun setsAreUniqueAndFit() {
        val rs = Book.recipes.filter { it.id != "great" }
        rs.forEach { assertTrue("${it.id} needs 2..3 kinds", it.needs.size in 2..3) }
        assertEquals(rs.size, rs.map { Lab.key(it.needs.keys) }.toSet().size)
        rs.forEach { assertEquals(it.id, Lab.match(it.needs.keys)?.id) }
    }

    @Test fun everythingAskedForExists() {
        (Book.recipes.flatMap { it.needs.keys } + Decors.all.flatMap { it.needs.keys }).forEach { assertTrue(it, it in Book.ing) }
    }

    @Test fun hintsTellWarmFromCold() {
        assertTrue(Lab.hint(listOf("moon", "wax"), setOf("w_lantern")).isNotEmpty())
        // moon + wax is part of «beast's heart» (moon, wax, stone): something is missing
        assertEquals(Lab.hint(listOf("resin", "sun"), emptySet()), Lab.hint(listOf("shard", "dawn"), emptySet()))
    }
}
