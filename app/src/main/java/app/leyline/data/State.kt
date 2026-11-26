package app.leyline.data

import app.leyline.domain.Kind
import app.leyline.domain.Mount
import app.leyline.domain.Sanctum
import app.leyline.domain.Station
import org.json.JSONArray
import org.json.JSONObject

/** A quarter of the real city: a named place from OpenStreetMap; its kind is learnt on the first visit. */
data class Quarter(
    val id: String,
    val ru: String,
    val en: String,
    val lat: Double,
    val lon: Double,
    val kind: Kind? = null,
    val second: Kind? = null,
    val visits: Int = 0,
    val firstDay: Long = 0,
    /** The kind is a stand-in: the map server didn't answer, it will be asked again. */
    val guessed: Boolean = false,
    /** Times its kind was changed by hand: the first is free, the rest cost [app.leyline.domain.Quarters.REKIND] power. */
    val changes: Int = 0,
) {
    val name get() = app.leyline.domain.tr(ru, en)
    val known get() = kind != null
}

/** A ride through the city, from «set off» to «arrived», and what it brought. */
data class Trip(
    val start: Long,
    val end: Long = 0,
    val ride: String = "car",
    val meters: Double = 0.0,
    /** Metres at 50 km/h and faster. */
    val fastMeters: Double = 0.0,
    val jamSec: Int = 0,
    /** Seconds standing still since the last move; they become jam time only if the ride moves on soon. */
    val jamWait: Int = 0,
    val underground: Int = 0,
    val weather: String = "",
    val temp: Double = Double.NaN,
    val firstLat: Double = 0.0,
    val firstLon: Double = 0.0,
    val lastLat: Double = 0.0,
    val lastLon: Double = 0.0,
    val lastAt: Long = 0,
    /** The route, a point every 100 m or so (dropped from old trips). */
    val pts: List<Pair<Double, Double>> = emptyList(),
    val quarters: List<String> = emptyList(),
    val fresh: List<String> = emptyList(),
    val newCells: Int = 0,
    val knots: Int = 0,
    val night: Boolean = false,
    val twilight: Boolean = false,
    val fullMoon: Boolean = false,
    /** Ingredients brought. */
    val gains: Map<String, Int> = emptyMap(),
    val power: Int = 0,
    val xp: Double = 0.0,
    /** The map along the route: 0 still to ask, 1 read, 2 asked and failed (asked again later). */
    val osm: Int = 0,
    val found: List<String> = emptyList(),
    val met: List<String> = emptyList(),
    /** Little events of the ride, "по-русски|in English". */
    val notes: List<String> = emptyList(),
    /** Ingredients doubled by the harvest elixir on this ride (also applies to the map's share). */
    val harvest: Boolean = false,
    val seeker: Boolean = false,
    val endSanctum: Long = 0,
    val startSanctum: Long = 0,
) {
    val km get() = meters / 1000
    val beast get() = app.leyline.domain.Ride.of(ride).beast
}

/**
 * A resident's story in quarter [quarter] (see [app.leyline.domain.Folk]): resident [who], at request [step];
 * [since] is when it was taken (0 – not yet), [target] the letter's quarter, [arrived] – the letter is delivered
 * or the far ride made.
 */
data class Errand(val quarter: String, val who: Int, val step: Int = 0, val since: Long = 0, val target: String = "", val arrived: Boolean = false)

data class State(
    val onboarded: Boolean = false,
    /** The grid's origin: where the first home was. */
    val homeLat: Double = 0.0,
    val homeLon: Double = 0.0,
    val quarters: Map<String, Quarter> = emptyMap(),
    val here: String = "",
    val power: Int = 0,
    val bag: Map<String, Int> = emptyMap(),
    /** Ingredients ever gathered: a recipe opens when all of its own are here. */
    val seen: Set<String> = emptySet(),
    val known: Set<String> = emptySet(),
    val stock: Map<String, Int> = emptyMap(),
    val brewed: Map<String, Int> = emptyMap(),
    /** Elixirs at work: id → rides left. */
    val buffs: Map<String, Int> = emptyMap(),
    val mounts: Map<String, Mount> = emptyMap(),
    /** The beast shown at home. */
    val beast: String = "car",
    val cells: Map<String, Int> = emptyMap(),
    val ley: Map<String, Int> = emptyMap(),
    /** Threads under the ground (the underground): dashed on the map. */
    val deep: Map<String, Int> = emptyMap(),
    val sanctums: List<Sanctum> = emptyList(),
    val trips: List<Trip> = emptyList(),
    val trip: Trip? = null,
    /** Spirits met: id → the step they're on (4 = friend). */
    val spirits: Map<String, Int> = emptyMap(),
    val spiritCount: Map<String, Int> = emptyMap(),
    val spiritSince: Map<String, Long> = emptyMap(),
    val told: Set<String> = emptySet(),
    val theme: String = "",
    val voice: Boolean = false,
    val ride: String = "car",
    val nextId: Long = 1,
    val btAddr: String = "",
    val btName: String = "",
    /** How far around home the quarters are loaded, metres. */
    val reach: Int = 0,
    /** Wonders brewed and switched on. */
    val aurora: Boolean = false,
    val mirror: Boolean = false,
    val great: Boolean = false,
    /** The cell of the last fix on this trip. */
    val lastCell: String = "",
    /** Sets of ingredients already tried in the cauldron that made a dud ("a+b+c"). */
    val tried: Set<String> = emptySet(),
    /** Cozy things bought for the tower. */
    val decor: Set<String> = emptySet(),
    /** The day the blooming quarter last gave its gift. */
    val bloomDay: Long = 0,
    /** The underground's stations around home, for rides by metro, and when they were loaded. */
    val stations: List<Station> = emptyList(),
    val stationsAt: Long = 0,
    /** The residents and their errands. */
    val folk: List<Errand> = emptyList(),
) {
    fun has(ing: String) = bag[ing] ?: 0
    fun friends() = spirits.filterValues { it >= 4 }.keys
}

// ---- JSON, by hand: small, readable and stable across versions ----

private fun JSONObject.str(k: String) = optString(k, "")
private fun <T> JSONArray?.list(f: (JSONObject) -> T): List<T> = if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
private fun JSONObject?.ints(): Map<String, Int> = this?.let { m -> m.keys().asSequence().associateWith { m.getInt(it) } }.orEmpty()
private fun JSONObject?.longs(): Map<String, Long> = this?.let { m -> m.keys().asSequence().associateWith { m.getLong(it) } }.orEmpty()
private fun Map<String, *>.obj() = JSONObject(this as Map<*, *>)
private fun List<Pair<Double, Double>>.flat() = JSONArray().also { a -> forEach { a.put(Math.round(it.first * 1e5) / 1e5); a.put(Math.round(it.second * 1e5) / 1e5) } }
private fun JSONArray?.pairs(): List<Pair<Double, Double>> = if (this == null) emptyList() else (0 until length() / 2).map { getDouble(it * 2) to getDouble(it * 2 + 1) }

fun Quarter.json() = JSONObject().put("id", id).put("ru", ru).put("en", en).put("lat", lat).put("lon", lon)
    .put("kind", kind?.id ?: "").put("second", second?.id ?: "").put("visits", visits).put("firstDay", firstDay).put("guessed", guessed).put("changes", changes)

fun quarterOf(o: JSONObject) = Quarter(o.str("id"), o.str("ru"), o.str("en"), o.getDouble("lat"), o.getDouble("lon"),
    Kind.of(o.str("kind")), Kind.of(o.str("second")), o.optInt("visits"), o.optLong("firstDay"), o.optBoolean("guessed"), o.optInt("changes"))

fun Trip.json(): JSONObject = JSONObject().put("start", start).put("end", end).put("ride", ride).put("meters", meters).put("fast", fastMeters)
    .put("jam", jamSec).put("jamWait", jamWait).put("under", underground).put("weather", weather).put("temp", if (temp.isNaN()) JSONObject.NULL else temp)
    .put("fLat", firstLat).put("fLon", firstLon).put("lLat", lastLat).put("lLon", lastLon).put("lAt", lastAt).put("pts", pts.flat())
    .put("quarters", JSONArray(quarters)).put("fresh", JSONArray(fresh)).put("newCells", newCells).put("knots", knots)
    .put("night", night).put("twilight", twilight).put("full", fullMoon).put("gains", gains.obj()).put("power", power).put("xp", xp)
    .put("osm", osm).put("found", JSONArray(found)).put("met", JSONArray(met)).put("notes", JSONArray(notes))
    .put("harvest", harvest).put("seeker", seeker).put("endS", endSanctum).put("startS", startSanctum)

fun tripOf(o: JSONObject) = Trip(
    o.getLong("start"), o.optLong("end"), o.optString("ride", "car"), o.optDouble("meters", 0.0), o.optDouble("fast", 0.0),
    o.optInt("jam"), o.optInt("jamWait"), o.optInt("under"), o.str("weather"), if (o.isNull("temp")) Double.NaN else o.optDouble("temp", Double.NaN),
    o.optDouble("fLat", 0.0), o.optDouble("fLon", 0.0), o.optDouble("lLat", 0.0), o.optDouble("lLon", 0.0), o.optLong("lAt"),
    o.optJSONArray("pts").pairs(), o.optJSONArray("quarters").strings(), o.optJSONArray("fresh").strings(), o.optInt("newCells"), o.optInt("knots"),
    o.optBoolean("night"), o.optBoolean("twilight"), o.optBoolean("full"), o.optJSONObject("gains").ints(), o.optInt("power"), o.optDouble("xp", 0.0),
    o.optInt("osm"), o.optJSONArray("found").strings(), o.optJSONArray("met").strings(), o.optJSONArray("notes").strings(),
    o.optBoolean("harvest"), o.optBoolean("seeker"), o.optLong("endS"), o.optLong("startS"),
)

fun Mount.json() = JSONObject().put("beast", beast).put("km", km).put("stage", stage).put("rides", rides).put("nightKm", nightKm)
    .put("wet", wetRides).put("fastKm", fastKm).put("jamMin", jamMin).put("rivers", rivers).put("newCells", newCells)
    .put("born", born).put("last", lastRide).put("name", name)

fun mountOf(o: JSONObject) = Mount(o.str("beast"), o.optDouble("km", 0.0), o.optInt("stage"), o.optInt("rides"), o.optDouble("nightKm", 0.0),
    o.optInt("wet"), o.optDouble("fastKm", 0.0), o.optInt("jamMin"), o.optInt("rivers"), o.optInt("newCells"), o.optLong("born"), o.optLong("last"), o.str("name"))

fun Sanctum.json() = JSONObject().put("id", id).put("lat", lat).put("lon", lon).put("visits", visits).put("name", name)
    .put("ward", ward).put("wardLevel", wardLevel).put("since", since)

fun sanctumOf(o: JSONObject) = Sanctum(o.getLong("id"), o.getDouble("lat"), o.getDouble("lon"), o.optInt("visits", 1), o.str("name"),
    o.str("ward"), o.optInt("wardLevel"), o.optLong("since"))

fun Station.json() = JSONObject().put("ru", ru).put("en", en).put("lat", lat).put("lon", lon)

fun stationOf(o: JSONObject) = Station(o.str("ru"), o.str("en"), o.getDouble("lat"), o.getDouble("lon"))

fun Errand.json() = JSONObject().put("quarter", quarter).put("who", who).put("step", step).put("since", since).put("target", target).put("arrived", arrived)
fun errandOf(o: JSONObject) = Errand(o.str("quarter"), o.optInt("who"), o.optInt("step"), o.optLong("since"), o.str("target"), o.optBoolean("arrived"))

fun State.json(): JSONObject = JSONObject()
    .put("v", 1).put("onboarded", onboarded).put("homeLat", homeLat).put("homeLon", homeLon)
    .put("quarters", JSONArray(quarters.values.map { it.json() })).put("here", here).put("power", power)
    .put("bag", bag.obj()).put("seen", JSONArray(seen.toList())).put("known", JSONArray(known.toList()))
    .put("stock", stock.obj()).put("brewed", brewed.obj()).put("buffs", buffs.obj())
    .put("mounts", JSONArray(mounts.values.map { it.json() })).put("beast", beast)
    .put("cells", cells.obj()).put("ley", ley.obj()).put("deep", deep.obj())
    .put("sanctums", JSONArray(sanctums.map { it.json() })).put("trips", JSONArray(trips.map { it.json() }))
    .put("trip", trip?.json() ?: JSONObject.NULL)
    .put("spirits", spirits.obj()).put("spiritCount", spiritCount.obj()).put("spiritSince", spiritSince.obj())
    .put("told", JSONArray(told.toList())).put("theme", theme).put("voice", voice).put("ride", ride).put("nextId", nextId)
    .put("btAddr", btAddr).put("btName", btName).put("reach", reach).put("placesV", 2).put("aurora", aurora).put("mirror", mirror).put("great", great).put("lastCell", lastCell)
    .put("tried", JSONArray(tried.toList())).put("decor", JSONArray(decor.toList())).put("bloomDay", bloomDay)
    .put("stations", JSONArray(stations.map { it.json() })).put("stationsAt", stationsAt)
    .put("folk", JSONArray(folk.map { it.json() }))

fun stateOf(o: JSONObject) = State(
    onboarded = o.optBoolean("onboarded"), homeLat = o.optDouble("homeLat", 0.0), homeLon = o.optDouble("homeLon", 0.0),
    quarters = o.optJSONArray("quarters").list(::quarterOf).associateBy { it.id }, here = o.str("here"), power = o.optInt("power"),
    bag = o.optJSONObject("bag").ints(), seen = o.optJSONArray("seen").strings().toSet(), known = o.optJSONArray("known").strings().toSet(),
    stock = o.optJSONObject("stock").ints(), brewed = o.optJSONObject("brewed").ints(), buffs = o.optJSONObject("buffs").ints(),
    mounts = o.optJSONArray("mounts").list(::mountOf).associateBy { it.beast }, beast = o.optString("beast", "car"),
    cells = o.optJSONObject("cells").ints(), ley = o.optJSONObject("ley").ints(), deep = o.optJSONObject("deep").ints(),
    sanctums = o.optJSONArray("sanctums").list(::sanctumOf), trips = o.optJSONArray("trips").list(::tripOf),
    trip = o.optJSONObject("trip")?.let(::tripOf),
    spirits = o.optJSONObject("spirits").ints(), spiritCount = o.optJSONObject("spiritCount").ints(), spiritSince = o.optJSONObject("spiritSince").longs(),
    told = o.optJSONArray("told").strings().toSet(), theme = o.str("theme"), voice = o.optBoolean("voice"), ride = o.optString("ride", "car"),
    nextId = o.optLong("nextId", 1), btAddr = o.str("btAddr"), btName = o.str("btName"),
    reach = if (o.optInt("placesV", 1) < 2 || (o.optJSONArray("quarters")?.length() ?: 0) == 0) 0 else o.optInt("reach", 9000),
    aurora = o.optBoolean("aurora"), mirror = o.optBoolean("mirror"), great = o.optBoolean("great"), lastCell = o.str("lastCell"),
    tried = o.optJSONArray("tried").strings().toSet(), decor = o.optJSONArray("decor").strings().toSet(), bloomDay = o.optLong("bloomDay"),
    stations = o.optJSONArray("stations").list(::stationOf), stationsAt = o.optLong("stationsAt"),
    folk = o.optJSONArray("folk").list(::errandOf),
)
