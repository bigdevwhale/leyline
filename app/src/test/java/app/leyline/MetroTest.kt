package app.leyline

import app.leyline.domain.Metro
import app.leyline.domain.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetroTest {
    private val lubyanka = Station("Лубянка", "Lubyanka", 55.75990, 37.62520)
    private val kuznetsky = Station("Кузнецкий Мост", "Kuznetsky Most", 55.76170, 37.62440)
    private val park = Station("Парк культуры", "Park Kultury", 55.73550, 37.59380)

    /** A node, a building and a stop area of one station make one station; a namesake far off stays. */
    @Test fun oneStationPerPlace() {
        val twin = lubyanka.copy(lat = 55.7601, lon = 37.6260)
        val far = park.copy(lat = 55.7360, lon = 37.5950)
        val other = park.copy(lat = 55.7120, lon = 37.5600)
        val got = Metro.dedupe(listOf(lubyanka, twin, park, far, kuznetsky, other))
        assertEquals(listOf("Кузнецкий Мост", "Лубянка", "Парк культуры", "Парк культуры"), got.map { it.ru })
    }

    @Test fun snapsOnlyWhenNear() {
        val all = listOf(lubyanka, kuznetsky, park)
        assertEquals(lubyanka, Metro.nearest(all, 55.7597, 37.6255))
        assertNull(Metro.nearest(all, 55.7480, 37.6100))
    }

    @Test fun searchesBothNamesNearestFirst() {
        val all = listOf(park, lubyanka, kuznetsky)
        assertEquals(listOf(kuznetsky), Metro.pick(all, "мост", null, null))
        assertEquals(listOf(park), Metro.pick(all, "kult", null, null))
        assertEquals(listOf(lubyanka, kuznetsky, park), Metro.pick(all, "", 55.7598, 37.6252))
    }
}
