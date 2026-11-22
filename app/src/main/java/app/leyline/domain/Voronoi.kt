package app.leyline.domain

import app.leyline.data.Quarter
import app.leyline.trip.Geo

/**
 * The quarters' borders: each place owns what is nearer to it than to any other place, cut to [Quarters.REACH]
 * around it (an octagon), so the city's edge doesn't run out to the horizon.
 */
object Voronoi {
    /** Each quarter's outline as lat/lon pairs. */
    fun cells(qs: Collection<Quarter>): Map<String, List<Pair<Double, Double>>> {
        if (qs.isEmpty()) return emptyMap()
        val lat0 = qs.map { it.lat }.average(); val lon0 = qs.map { it.lon }.average()
        val pts = qs.map { it.id to Geo.local(lat0, lon0, it.lat, it.lon) }
        val r = Quarters.REACH
        return pts.associate { (id, p) ->
            var poly = (0 until 8).map { i -> val a = Math.PI / 8 + i * Math.PI / 4; p.first + r * 1.08 * Math.cos(a) to p.second + r * 1.08 * Math.sin(a) }
            for ((oid, o) in pts) {
                if (oid == id) continue
                val dx = o.first - p.first; val dy = o.second - p.second
                if (dx * dx + dy * dy > 4 * r * r) continue
                // keep the side of the bisector nearer to p: (x - m)·d <= 0
                val mx = (p.first + o.first) / 2; val my = (p.second + o.second) / 2
                poly = clip(poly) { x, y -> (x - mx) * dx + (y - my) * dy }
                if (poly.isEmpty()) break
            }
            id to poly.map { (x, y) -> Geo.unlocal(lat0, lon0, x, y) }
        }
    }

    /** Sutherland–Hodgman against one half-plane f(x, y) <= 0. */
    private fun clip(poly: List<Pair<Double, Double>>, f: (Double, Double) -> Double): List<Pair<Double, Double>> {
        val out = ArrayList<Pair<Double, Double>>(poly.size + 2)
        for (i in poly.indices) {
            val a = poly[i]; val b = poly[(i + 1) % poly.size]
            val fa = f(a.first, a.second); val fb = f(b.first, b.second)
            if (fa <= 0) out += a
            if ((fa <= 0) != (fb <= 0)) {
                val t = fa / (fa - fb)
                out += (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
            }
        }
        return out
    }
}
