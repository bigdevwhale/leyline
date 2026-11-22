package app.leyline.trip

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_M = 6_371_008.8

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dp = p2 - p1
        val dl = (lon2 - lon1) * PI / 180
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Metres east and north of ([lat0], [lon0]); fine across a city. */
    fun local(lat0: Double, lon0: Double, lat: Double, lon: Double): Pair<Double, Double> =
        (lon - lon0) * PI / 180 * EARTH_M * cos(lat0 * PI / 180) to (lat - lat0) * PI / 180 * EARTH_M

    fun unlocal(lat0: Double, lon0: Double, x: Double, y: Double): Pair<Double, Double> =
        lat0 + y / EARTH_M * 180 / PI to lon0 + x / (EARTH_M * cos(lat0 * PI / 180)) * 180 / PI
}

/** A position from the phone. */
data class Fix(val lat: Double, val lon: Double, val accuracy: Float, val time: Long, val speed: Float? = null, val gps: Boolean = true, val simulated: Boolean = false)
