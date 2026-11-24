package app.leyline.domain

import app.leyline.trip.Geo
import java.util.Locale

/** A station of the underground, from OpenStreetMap. */
data class Station(val ru: String, val en: String, val lat: Double, val lon: Double) {
    val name get() = tr(ru, en)
}

/**
 * The underground has no sky, so a ride by metro doesn't keep the GPS on: one position at the entrance, one at
 * the exit, and the stations of the city to pick from when the satellites can't be found.
 */
object Metro {
    /** A position this good (metres) is enough to know the station. */
    const val GOOD = 150f
    /** A position snaps to a station this near. */
    const val SNAP = 400.0
    /** Stations are loaded this far around home. */
    const val RADIUS = 40_000

    /** The city's stations around ([lat], [lon]), or null without the network. */
    suspend fun around(lat: Double, lon: Double, radius: Int = RADIUS): List<Station>? {
        val at = String.format(Locale.US, "%.5f,%.5f", lat, lon)
        val js = Quarters.ask("[out:json][timeout:40];(" +
            "nwr(around:$radius,$at)[railway=station][station~\"^(subway|light_rail|monorail)$\"][name];" +
            "nwr(around:$radius,$at)[public_transport=station][subway=yes][name];" +
            ");out center tags;") ?: return null
        val els = js.optJSONArray("elements") ?: return null
        return dedupe((0 until els.length()).mapNotNull { i ->
            val e = els.getJSONObject(i)
            val t = e.optJSONObject("tags") ?: return@mapNotNull null
            val name = t.optString("name").ifEmpty { return@mapNotNull null }
            val c = e.optJSONObject("center")
            val la = c?.optDouble("lat") ?: e.optDouble("lat", Double.NaN)
            val lo = c?.optDouble("lon") ?: e.optDouble("lon", Double.NaN)
            if (la.isNaN() || lo.isNaN()) return@mapNotNull null
            val en = t.optString("name:en").ifEmpty { name }
            Station(t.optString("name:ru").ifEmpty { en }, en, la, lo)
        })
    }

    /** One station per name and place: OpenStreetMap often has a node, a building and a stop area for each. */
    fun dedupe(all: List<Station>): List<Station> {
        val out = ArrayList<Station>()
        for (s in all) if (out.none { it.ru == s.ru && Geo.distance(it.lat, it.lon, s.lat, s.lon) < 600 }) out += s
        return out.sortedBy { it.ru }
    }

    fun nearest(list: List<Station>, lat: Double, lon: Double, within: Double = SNAP): Station? =
        list.minByOrNull { Geo.distance(it.lat, it.lon, lat, lon) }?.takeIf { Geo.distance(it.lat, it.lon, lat, lon) <= within }

    /** Stations matching [query] (any part of either name), nearest to [lat], [lon] first when it is known. */
    fun pick(list: List<Station>, query: String, lat: Double?, lon: Double?): List<Station> {
        val q = query.trim().lowercase()
        val hit = if (q.isEmpty()) list else list.filter { q in it.ru.lowercase() || q in it.en.lowercase() }
        return if (lat == null || lon == null) hit else hit.sortedBy { Geo.distance(it.lat, it.lon, lat, lon) }
    }
}
