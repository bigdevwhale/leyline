package app.leyline.domain

import app.leyline.data.Quarter
import app.leyline.trip.Geo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * The city's quarters come from OpenStreetMap: named suburbs, quarters and neighbourhoods around the merchant.
 * Each point owns the streets nearer to it than to any other (a Voronoi map). What a quarter is – gardens,
 * forges, a bazaar – is counted from what lies within 700 m of it, on the first visit.
 */
object Quarters {
    private val ENDPOINTS = listOf("https://overpass-api.de/api/interpreter", "https://overpass.private.coffee/api/interpreter", "https://overpass.kumi.systems/api/interpreter", "https://maps.mail.ru/osm/tools/overpass/api/interpreter")
    const val REACH = 4000.0
    /** Radii the quarters are loaded in, smallest first. */
    val RINGS = listOf(3000, 6000, 9000)

    /**
     * Asks every mirror at once and takes the first good answer: public Overpass servers are often busy. Gives up
     * after [timeout] (and the time to connect) in all: a busy server trickles its answer out, and a blocking read
     * neither notices a cancelled coroutine nor its own read timeout then – so the connections are cut by hand.
     */
    suspend fun ask(q: String, timeout: Int = 40_000): JSONObject? = coroutineScope {
        val body = "data=" + URLEncoder.encode(q, "UTF-8")
        val done = CompletableDeferred<JSONObject?>()
        val open = java.util.concurrent.CopyOnWriteArrayList<HttpURLConnection>()
        val jobs = ENDPOINTS.map { url ->
            launch(Dispatchers.IO) {
                val r = runCatching {
                    val c = URL(url).openConnection() as HttpURLConnection
                    open += c
                    c.connectTimeout = 10_000; c.readTimeout = timeout; c.requestMethod = "POST"; c.doOutput = true
                    c.setRequestProperty("User-Agent", "Leyline (app.leyline; github.com/bigdevwhale/leyline)")
                    c.outputStream.use { it.write(body.toByteArray()) }
                    if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                    JSONObject(c.inputStream.bufferedReader().use { it.readText() }).takeIf { it.has("elements") && !it.optString("remark").contains("error", ignoreCase = true) }
                }.getOrNull()
                if (r != null) done.complete(r)
            }
        }
        val all = launch { jobs.joinAll(); done.complete(null) }
        try {
            withTimeoutOrNull(timeout + 10_000L) { done.await() }
        } finally {
            all.cancel(); jobs.forEach { it.cancel() }
            open.forEach { runCatching { it.disconnect() } }
        }
    }

    /** Named places within [radius] m, or null without the network. */
    suspend fun around(lat: Double, lon: Double, radius: Int = 9_000): List<Quarter>? {
        val at = String.format(Locale.US, "%.5f,%.5f", lat, lon)
        val js = ask("[out:json][timeout:30];node(around:$radius,$at)[place~\"^(suburb|quarter|neighbourhood|city|town|village|hamlet)$\"][name];out;") ?: return null
        val els = js.optJSONArray("elements") ?: return null
        val all = (0 until els.length()).map { els.getJSONObject(it) }.mapNotNull { e ->
            val t = e.optJSONObject("tags") ?: return@mapNotNull null
            val name = t.optString("name")
            if (name.isEmpty()) return@mapNotNull null
            val en = t.optString("name:en").ifEmpty { name }
            // a place with no Russian name shows its English one before the local script (Hebrew, Arabic…)
            val ru = t.optString("name:ru").ifEmpty { en }
            Triple(t.optString("place"), Quarter(e.getLong("id").toString(), ru, en, e.getDouble("lat"), e.getDouble("lon")), name)
        }
        // Inside a city its districts come first, neighbourhoods fill in where they are few; beyond it towns, villages
        // and hamlets are quarters of their own. A city's own centre point counts only where it has no districts.
        // Never closer than 600 m to one already kept.
        val rank = mapOf("suburb" to 0, "quarter" to 1, "neighbourhood" to 2, "town" to 3, "village" to 4, "hamlet" to 5, "city" to 6)
        val ordered = all.sortedBy { rank[it.first] ?: 9 }.map { it.second }
        val kept = ArrayList<Quarter>()
        for (q in ordered) if (kept.none { Geo.distance(it.lat, it.lon, q.lat, q.lon) < 600 }) kept += q
        return kept
    }

    private val CATS = linkedMapOf(
        Kind.GARDEN to ("nwr(around:R,P)[leisure~\"^(park|garden)$\"];way(around:R,P)[landuse~\"^(forest|grass|meadow|recreation_ground)$\"];way(around:R,P)[natural=wood];" to 1 / 100.0),
        Kind.WATER to ("way(around:R,P)[waterway~\"^(river|canal)$\"];nwr(around:R,P)[natural=water];way(around:R,P)[man_made=pier];" to 1 / 8.0),
        Kind.OLD to ("nwr(around:R,P)[historic];nwr(around:R,P)[tourism=museum];nwr(around:R,P)[amenity=place_of_worship];" to 1 / 40.0),
        Kind.SCHOLAR to ("nwr(around:R,P)[amenity~\"^(university|college|library)$\"];nwr(around:R,P)[office=research];" to 1 / 8.0),
        Kind.FORGE to ("way(around:R,P)[landuse~\"^(industrial|railway)$\"];nwr(around:R,P)[man_made~\"^(works|chimney)$\"];nwr(around:R,P)[craft];" to 1 / 20.0),
        Kind.BAZAAR to ("nwr(around:R,P)[amenity=marketplace];nwr(around:R,P)[shop~\"^(mall|department_store)$\"];node(around:R,P)[shop];" to 1 / 300.0),
        Kind.GATE to ("nwr(around:R,P)[railway~\"^(station|halt)$\"];nwr(around:R,P)[amenity=bus_station];nwr(around:R,P)[public_transport=station];" to 1 / 2.0),
        Kind.HOMES to ("way(around:R,P)[building~\"^(apartments|residential|house)$\"];" to 1 / 200.0),
    )

    /** The quarter's kind and second kind from what is in it; a guess from its id without the network. */
    /** The kind and second kind, or null when the servers didn't answer (then [guess] stands in for a while). */
    suspend fun classify(q: Quarter): Pair<Kind, Kind>? {
        val p = String.format(Locale.US, "%.5f,%.5f", q.lat, q.lon)
        val query = "[out:json][timeout:50];" + CATS.entries.joinToString("") { (k, v) ->
            "(${v.first.replace("R,P", "700,$p")});out count;"
        }
        // busy servers must not keep a quarter unknown: after 30 s it gets its kind by the fallback rule
        val js = kotlinx.coroutines.withTimeoutOrNull(30_000) { ask(query, 28_000) }
        val els = js?.optJSONArray("elements")
        if (els == null || els.length() < CATS.size) return null
        val scores = CATS.entries.mapIndexed { i, (k, v) ->
            val n = els.getJSONObject(i).optJSONObject("tags")?.optString("total")?.toIntOrNull() ?: 0
            k to n * v.second
        }.sortedByDescending { it.second }
        if (scores.first().second <= 0.05) return guess(q)
        return scores[0].first to scores[1].first
    }

    fun guess(q: Quarter): Pair<Kind, Kind> {
        val r = Rand(hash(q.id))
        val a = Kind.entries[r.int(Kind.entries.size)]
        return a to a.near[r.int(2)]
    }

    /** A ride this far from every quarter opens a new one on the spot. */
    const val GAP = 1500.0

    private val WILD = listOf(
        "Тихая поляна" to "Quiet glade", "Лесная развилка" to "Forest fork", "Старый тракт" to "Old track",
        "Сосновый перекрёсток" to "Pine crossing", "Ветреный холм" to "Windy hill", "Дальний хутор" to "Far farmstead",
        "Берёзовая опушка" to "Birch edge", "Каменный брод" to "Stone ford", "Совиный лог" to "Owl hollow",
        "Медвежий угол" to "Bear's corner", "Туманная низина" to "Misty lowland", "Ягодная просека" to "Berry clearing",
    )

    /**
     * Where the map has no named places for kilometres (forests, fields, the edges of town) a ride opens a quarter
     * right where it is, named after what is near: a hamlet, a station, a dacha settlement, a park, a wood or a
     * lake, else the road; with nothing named around, a name of the wilds. It always comes back, even offline.
     */
    suspend fun spot(lat: Double, lon: Double, taken: Set<String>): Quarter {
        val p = String.format(Locale.US, "%.5f,%.5f", lat, lon)
        val id = "g" + String.format(Locale.US, "%.4f_%.4f", lat, lon)
        val q = "[out:json][timeout:30];(" +
            "nwr(around:800,$p)[place~\"^(locality|isolated_dwelling|allotments|farm|hamlet|village|neighbourhood|quarter|suburb)$\"][name];" +
            "node(around:800,$p)[railway~\"^(station|halt)$\"][name];" +
            "nwr(around:800,$p)[landuse=allotments][name];" +
            "nwr(around:800,$p)[leisure~\"^(park|nature_reserve)$\"][name];" +
            "nwr(around:800,$p)[natural~\"^(wood|water)$\"][name];nwr(around:800,$p)[landuse=forest][name];" +
            "way(around:300,$p)[highway][name];);out tags center;"
        val els = kotlinx.coroutines.withTimeoutOrNull(45_000) { ask(q, 40_000) }?.optJSONArray("elements")
        val best = (0 until (els?.length() ?: 0)).mapNotNull { i ->
            val e = els!!.getJSONObject(i)
            val t = e.optJSONObject("tags") ?: return@mapNotNull null
            val name = t.optString("name").ifEmpty { return@mapNotNull null }
            val en = t.optString("name:en").ifEmpty { name }
            val ru = t.optString("name:ru").ifEmpty { en }
            if (ru in taken) return@mapNotNull null
            val rank = when {
                t.has("place") -> 0; t.has("railway") -> 1; t.optString("landuse") == "allotments" -> 2
                t.has("leisure") -> 3; t.has("highway") -> 5; else -> 4
            }
            val c = e.optJSONObject("center")
            val elat = if (e.has("lat")) e.getDouble("lat") else c?.optDouble("lat") ?: lat
            val elon = if (e.has("lon")) e.getDouble("lon") else c?.optDouble("lon") ?: lon
            Triple(rank, Geo.distance(lat, lon, elat, elon), ru to en)
        }.minWithOrNull(compareBy({ it.first }, { it.second }))?.third
        // the quarter stands where the ride is, so it never crowds a neighbour, whatever its name's centre
        val (ru, en) = best ?: WILD.filter { it.first !in taken }.ifEmpty { WILD }.let { it[Rand(hash(id)).int(it.size)] }
        return Quarter(id, ru, en, lat, lon)
    }

    /** The quarter that owns this point: the nearest one within [REACH]. */
    fun at(quarters: Collection<Quarter>, lat: Double, lon: Double): Quarter? =
        quarters.minByOrNull { Geo.distance(lat, lon, it.lat, it.lon) }?.takeIf { Geo.distance(lat, lon, it.lat, it.lon) <= REACH }
}
