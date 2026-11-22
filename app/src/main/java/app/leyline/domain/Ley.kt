package app.leyline.domain

import app.leyline.trip.Geo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max

/**
 * The city's hidden net. The map is cut into 250 m cells around the first home; a ride lights the cells it
 * passes and the threads between them. A thread grows with every ride along it.
 */
object Ley {
    const val CELL = 250.0

    data class Cell(val x: Int, val y: Int) {
        val key get() = "$x,$y"
        companion object { fun of(k: String) = k.split(',').let { Cell(it[0].toInt(), it[1].toInt()) } }
    }

    fun cell(lat0: Double, lon0: Double, lat: Double, lon: Double): Cell {
        val (x, y) = Geo.local(lat0, lon0, lat, lon)
        return Cell(floor(x / CELL).toInt(), floor(y / CELL).toInt())
    }

    /** The middle of a cell as lat/lon. */
    fun center(lat0: Double, lon0: Double, c: Cell): Pair<Double, Double> = Geo.unlocal(lat0, lon0, (c.x + 0.5) * CELL, (c.y + 0.5) * CELL)

    fun edge(a: Cell, b: Cell): String = if (a.key < b.key) "${a.key}|${b.key}" else "${b.key}|${a.key}"

    /** The cells on the way from [a] to [b] (a straight line, both ends included). */
    fun walk(a: Cell, b: Cell): List<Cell> {
        val n = max(abs(b.x - a.x), abs(b.y - a.y))
        if (n == 0) return listOf(a)
        return (0..n).map { i -> Cell(a.x + Math.round((b.x - a.x) * i / n.toDouble()).toInt(), a.y + Math.round((b.y - a.y) * i / n.toDouble()).toInt()) }.distinct()
    }

    /** A thread's strength by rides along it. */
    fun level(n: Int) = when { n >= 20 -> 3; n >= 8 -> 2; n >= 3 -> 1; else -> 0 }
    fun levelName(l: Int) = when (l) { 3 -> tr("великая жила", "a great line"); 2 -> tr("жила", "a ley line"); 1 -> tr("тропа", "a path"); else -> tr("тропка", "a trail") }

    /** Knots: cells where three or more strong threads (3 rides and more) meet. */
    fun knots(ley: Map<String, Int>): Set<String> {
        val deg = HashMap<String, Int>()
        for ((k, n) in ley) if (n >= 3) k.split('|').forEach { deg[it] = (deg[it] ?: 0) + 1 }
        return deg.filterValues { it >= 3 }.keys
    }

    /** Kilometres of lit threads. */
    fun km(ley: Map<String, Int>) = ley.size * CELL / 1000
}

/** A place where rides keep starting and ending: home, work, a friend's. After three visits it awakens. */
data class Sanctum(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val visits: Int = 1,
    val name: String = "",
    val ward: String = "",
    val wardLevel: Int = 0,
    val since: Long = 0,
) {
    val awake get() = visits >= Sanctums.AWAKE
    fun title(quarter: String?): String = name.ifEmpty { quarter?.let { tr("Святилище · $it", "Sanctum · $it") } ?: tr("Святилище", "Sanctum") }
}

object Sanctums {
    const val AWAKE = 3
    const val REACH = 300.0

    fun near(list: List<Sanctum>, lat: Double, lon: Double): Sanctum? =
        list.filter { Geo.distance(it.lat, it.lon, lat, lon) < REACH }.minByOrNull { Geo.distance(it.lat, it.lon, lat, lon) }

    /** A ride began or ended here: the nearest sanctum gets a visit (its centre drifts toward the point), or a new one is born. */
    fun visit(list: List<Sanctum>, lat: Double, lon: Double, nextId: Long, at: Long): Pair<List<Sanctum>, Sanctum> {
        val s = near(list, lat, lon)
        if (s != null) {
            val k = 1.0 / (s.visits + 1).coerceAtMost(8)
            val upd = s.copy(visits = s.visits + 1, lat = s.lat + (lat - s.lat) * k, lon = s.lon + (lon - s.lon) * k)
            return list.map { if (it.id == s.id) upd else it } to upd
        }
        val n = Sanctum(nextId, lat, lon, 1, since = at)
        // a list of a hundred at most: one-off stops fade first
        val kept = (list + n).sortedWith(compareByDescending<Sanctum> { it.ward.isNotEmpty() }.thenByDescending { it.visits }.thenByDescending { it.since }).take(100)
        return kept to n
    }

    /** The sigil: up to six awakened sanctums, joined in order around their middle. Points are -1..1. */
    fun sigil(list: List<Sanctum>): List<Pair<Float, Float>> {
        val top = list.filter { it.awake }.sortedByDescending { it.visits }.take(6)
        if (top.size < 2) return emptyList()
        val lat0 = top.map { it.lat }.average(); val lon0 = top.map { it.lon }.average()
        val pts = top.map { Geo.local(lat0, lon0, it.lat, it.lon) }
        val r = pts.maxOf { kotlin.math.hypot(it.first, it.second) }.coerceAtLeast(1.0)
        return pts.sortedBy { atan2(it.second, it.first) }.map { (x, y) -> (x / r).toFloat() to (-y / r).toFloat() }
    }
}
