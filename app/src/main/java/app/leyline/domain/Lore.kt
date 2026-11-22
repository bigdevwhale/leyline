package app.leyline.domain

import java.util.Locale

/**
 * What the road passed, read from OpenStreetMap after the ride: bridges and rivers, parks, churches, old
 * buildings, universities, factories, stations, cemeteries, shops. One Overpass request along the route.
 */
object Lore {
    private class Q(val ing: String, val radius: Int, val parts: String, val per: Double, val cap: Int)

    private val QS = listOf(
        Q("mist", 30, "way(around:R,L)[bridge=yes][highway];way(around:R,L)[waterway~\"^(river|canal)$\"];", 1.0, 3),
        Q("resin", 50, "way(around:R,L)[leisure~\"^(park|garden)$\"];way(around:R,L)[landuse=forest];way(around:R,L)[natural=wood];", 2.0, 3),
        Q("wax", 80, "nwr(around:R,L)[amenity=place_of_worship];", 1.0, 2),
        Q("dust", 50, "nwr(around:R,L)[historic];nwr(around:R,L)[tourism=museum];", 3.0, 3),
        Q("ink", 80, "nwr(around:R,L)[amenity~\"^(university|college|library|school)$\"];", 2.0, 2),
        Q("spark", 60, "way(around:R,L)[landuse~\"^(industrial|railway)$\"];nwr(around:R,L)[man_made~\"^(works|chimney)$\"];", 2.0, 2),
        Q("feather", 120, "nwr(around:R,L)[railway~\"^(station|halt)$\"];nwr(around:R,L)[amenity=bus_station];", 1.0, 2),
        Q("lily", 80, "way(around:R,L)[landuse=cemetery];way(around:R,L)[amenity=grave_yard];", 1.0, 1),
        Q("copper", 30, "node(around:R,L)[shop];nwr(around:R,L)[amenity=marketplace];", 15.0, 3),
    )

    /** Points thinned to keep the request small: at most [max], at least 120 m apart. */
    fun thin(pts: List<Pair<Double, Double>>, max: Int = 90): List<Pair<Double, Double>> {
        if (pts.size <= 2) return pts
        val out = ArrayList<Pair<Double, Double>>()
        for (p in pts) if (out.isEmpty() || app.leyline.trip.Geo.distance(out.last().first, out.last().second, p.first, p.second) > 120) out += p
        if (out.size <= max) return out
        val step = out.size / max.toDouble()
        return (0 until max).map { out[(it * step).toInt()] } + out.last()
    }

    /** Counts per ingredient along the route, or null when the map servers didn't answer. */
    suspend fun along(pts: List<Pair<Double, Double>>): Map<String, Int>? {
        val line = thin(pts)
        if (line.size < 2) return emptyMap()
        val l = line.joinToString(",") { String.format(Locale.US, "%.5f,%.5f", it.first, it.second) }
        val q = "[out:json][timeout:50];" + QS.joinToString("") { "(${it.parts.replace("R,L", "${it.radius},$l")});out count;" }
        val js = kotlinx.coroutines.withTimeoutOrNull(45_000) { Quarters.ask(q, 40_000) } ?: return null
        val els = js.optJSONArray("elements") ?: return null
        if (els.length() < QS.size) return null
        return QS.mapIndexed { i, x ->
            val n = els.getJSONObject(i).optJSONObject("tags")?.optString("total")?.toIntOrNull() ?: 0
            x.ing to if (n <= 0) 0 else (1 + ((n - 1) / x.per).toInt()).coerceAtMost(x.cap)
        }.filter { it.second > 0 }.toMap()
    }
}
