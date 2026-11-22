package app.leyline.data

import app.leyline.trip.Fix
import kotlinx.coroutines.delay

/** Debug only: drives the cart from where it is to a point, fix by fix, at 40 km/h in fast-forward. */
object Sim {
    private var clock = 0L

    suspend fun driveTo(g: Game, lat: Double, lon: Double) {
        val from = g.live.value.fix ?: g.now.quarters[g.now.here]?.let { Fix(it.lat, it.lon, 5f, System.currentTimeMillis()) } ?: return
        if (clock < System.currentTimeMillis()) clock = System.currentTimeMillis()
        val d = app.leyline.trip.Geo.distance(from.lat, from.lon, lat, lon)
        val steps = (d / 100).toInt().coerceIn(2, 400)
        for (i in 1..steps) {
            val k = i.toDouble() / steps
            clock += (100 / 11.0 * 1000).toLong()
            g.onFix(Fix(from.lat + (lat - from.lat) * k, from.lon + (lon - from.lon) * k, 5f, clock, speed = 11f, simulated = true))
            delay(120)
        }
        clock += 5000
        g.onFix(Fix(lat, lon, 5f, clock, speed = 0f, simulated = true))
    }
}
