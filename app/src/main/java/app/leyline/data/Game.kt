package app.leyline.data

import android.content.Context
import app.leyline.domain.Ask
import app.leyline.domain.Book
import app.leyline.domain.Cat
import app.leyline.domain.Clues
import app.leyline.domain.Decor
import app.leyline.domain.Decors
import app.leyline.domain.Folk
import app.leyline.domain.Kind
import app.leyline.domain.Lab
import app.leyline.domain.Ley
import app.leyline.domain.Luck
import app.leyline.domain.Rand
import app.leyline.domain.Recipe
import app.leyline.domain.hash
import app.leyline.domain.Lore
import app.leyline.domain.Metro
import app.leyline.domain.Mount
import app.leyline.domain.Mounts
import app.leyline.domain.Omen
import app.leyline.domain.Pages
import app.leyline.domain.Quarters
import app.leyline.domain.Ride
import app.leyline.domain.Sanctum
import app.leyline.domain.Sanctums
import app.leyline.domain.Station
import app.leyline.domain.zone
import app.leyline.domain.Sky
import app.leyline.domain.Spirits
import app.leyline.domain.Weather
import app.leyline.domain.dayOf
import app.leyline.domain.today
import app.leyline.domain.tr
import app.leyline.trip.Fix
import app.leyline.trip.Geo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.Instant

/** What the ride is doing right now; never saved. */
data class Live(
    val fix: Fix? = null,
    /** m/s, smoothed. */
    val speed: Double = 0.0,
    val movingSince: Long = 0,
    val stillSince: Long = 0,
    val loading: Boolean = false,
    /** When the phone last gave any position at all, however rough (the clock's time). */
    val heardAt: Long = 0,
    /** When a rough position (too coarse to count metres) last showed the ride going on. */
    val stirAt: Long = 0,
) {
    /** The driver's screen rests while moving faster than a walk. */
    val moving get() = speed > 3.5
}

/** Something to say: a toast in the app, a line of the voice. */
class News(val text: String, val speak: Boolean = false)

/**
 * The whole game: one state, saved as JSON after every change, and the rules that change it. Writes go
 * through one lock, so the GPS, the cauldron and the trip's end never step on each other.
 */
class Game(private val app: Context, val scope: CoroutineScope) {
    private val file = File(app.filesDir, "leyline.json")
    private val lock = Mutex()
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
    val live = MutableStateFlow(Live())
    val news = MutableSharedFlow<News>(extraBufferCapacity = 16)
    /** The finished trip whose card should open, once (its start). */
    val finished = MutableStateFlow<Long?>(null)
    private var lastFetch = 0L
    private val classifying = HashSet<String>()
    private val tried = HashMap<String, Long>()
    private val asking = HashSet<Long>()
    private var knotCache: Pair<Int, Set<String>> = -1 to emptySet()

    val now: State get() = _state.value

    fun load() {
        _state.value = runCatching { stateOf(JSONObject(file.readText())) }.getOrNull() ?: State()
    }

    private fun save(s: State) {
        val text = s.json().toString()
        scope.launch(Dispatchers.IO) {
            runCatching {
                val tmp = File(file.parentFile, "leyline.json.tmp")
                tmp.writeText(text); tmp.renameTo(file) || run { file.writeText(text); true }
            }
        }
    }

    suspend fun edit(f: (State) -> State): State = lock.withLock {
        val s = upkeep(f(_state.value))
        _state.value = s
        save(s)
        s
    }

    fun write(f: (State) -> State) { scope.launch { edit(f) } }

    /**
     * What comes by itself after any change: a resident settles in a quarter visited often enough, a full page of
     * the grimoire gives its gift once (also to those whose pages were full before it came in).
     */
    private fun upkeep(s0: State): State {
        val s = Folk.settle(s0)
        return Pages.grant(s) { news.tryEmit(News(it)) }
    }

    // ---------- the city ----------

    /**
     * Sets home here, hatches the first beast and loads the first ring of quarters. True when the map answered;
     * without it home is still set and the quarters come later by themselves (see [grow]).
     */
    suspend fun settle(lat: Double, lon: Double, ride: Ride): Boolean {
        live.value = live.value.copy(loading = true)
        // the start waits 15 s at most: a slow map server is left to the background
        val qs = kotlinx.coroutines.withTimeoutOrNull(15_000) { Quarters.around(lat, lon, Quarters.RINGS.first()) }
        live.value = live.value.copy(loading = false)
        val home = qs?.let { Quarters.at(it, lat, lon) ?: it.minByOrNull { q -> Geo.distance(lat, lon, q.lat, q.lon) } }
        val t = System.currentTimeMillis()
        edit { s ->
            val merged = s.quarters.toMutableMap()
            qs?.forEach { q -> if (q.id !in merged) merged[q.id] = q }
            val b = ride.beast.id
            s.copy(homeLat = lat, homeLon = lon, quarters = merged, here = home?.id ?: s.here, ride = ride.id, beast = b,
                reach = if (!qs.isNullOrEmpty()) maxOf(s.reach, Quarters.RINGS.first()) else s.reach,
                mounts = if (b in s.mounts) s.mounts else s.mounts + (b to Mount(b, born = t)),
                // the first sanctum: where it all began
                sanctums = s.sanctums.ifEmpty { listOf(Sanctum(s.nextId, lat, lon, 1, tr("Дом", "Home"), since = t)) },
                nextId = s.nextId + 1)
        }
        home?.let { classify(it.id) }
        lastGrow = 0
        grow(lat, lon)
        return !qs.isNullOrEmpty()
    }


    private var growing = false
    private var lastGrow = 0L

    /**
     * Quarters load ring by ring: a small circle first (enough to start), then wider ones in the background.
     * A busy map server just stops the climb; it is tried again later (on the next start or position).
     */
    fun grow(lat: Double, lon: Double) {
        val t = System.currentTimeMillis()
        if (growing || t - lastGrow < 2 * 60_000L || lat == 0.0) return
        growing = true; lastGrow = t
        scope.launch {
            for (r in Quarters.RINGS) {
                if (now.reach >= r) continue
                val qs = Quarters.around(lat, lon, r) ?: break
                if (qs.isEmpty() && now.quarters.isEmpty() && r == Quarters.RINGS.last()) break
                edit { s -> s.copy(quarters = s.quarters + qs.filter { q -> q.id !in s.quarters && s.quarters.values.none { Geo.distance(it.lat, it.lon, q.lat, q.lon) < 600 } }.associateBy { it.id }, reach = maxOf(s.reach, r)) }
                // the first ring may have found the quarter we stand in
                if (now.here.isEmpty()) live.value.fix?.let { f -> Quarters.at(now.quarters.values, f.lat, f.lon) }?.let { q -> edit { it.copy(here = q.id) }; classify(q.id) }
            }
            growing = false
        }
    }

    private var spawning = false
    private var lastSpawn = 0L

    /** A ride far from every quarter (woods, fields, the town's edge) opens a new quarter where it is. */
    private fun spawn(lat: Double, lon: Double) {
        val t = System.currentTimeMillis()
        if (spawning || t - lastSpawn < 90_000) return
        spawning = true; lastSpawn = t
        scope.launch {
            try {
                val q = Quarters.spot(lat, lon, now.quarters.values.map { it.ru }.toSet())
                edit { s -> if (s.quarters.values.any { Geo.distance(it.lat, it.lon, q.lat, q.lon) < Quarters.GAP * 0.8 }) s else s.copy(quarters = s.quarters + (q.id to q)) }
            } finally { spawning = false }
        }
    }

    private fun fetchAround(lat: Double, lon: Double) {
        val t = System.currentTimeMillis()
        if (t - lastFetch < 10 * 60_000) return
        lastFetch = t
        scope.launch {
            val qs = Quarters.around(lat, lon, 8000) ?: return@launch
            edit { s -> s.copy(quarters = s.quarters + qs.filter { q -> q.id !in s.quarters && s.quarters.values.none { Geo.distance(it.lat, it.lon, q.lat, q.lon) < 600 } }.associateBy { it.id }) }
        }
    }

    fun ensureKnown() {
        val s = now
        if (s.onboarded && s.reach < Quarters.RINGS.last()) grow(s.homeLat, s.homeLon)
        s.quarters[s.here]?.takeIf { it.kind == null || (it.guessed && System.currentTimeMillis() - (tried[it.id] ?: 0) > 3 * 3600_000L) }?.let { classify(it.id) }
        s.trip?.quarters?.distinct()?.forEach { id -> if (s.quarters[id]?.kind == null) classify(id) }
        // rides whose map reading failed get another go – and those whose reading was cut short when the app was closed
        s.trips.filter { it.osm != 1 && System.currentTimeMillis() - it.end < 3 * 86_400_000L }.take(3).forEach { enrich(it.start) }
    }

    private fun classify(id: String) {
        if (!classifying.add(id)) return
        scope.launch {
            val q = now.quarters[id] ?: run { classifying.remove(id); return@launch }
            val real = Quarters.classify(q)
            val (a, b) = real ?: Quarters.guess(q)
            // a stand-in never replaces a kind already there. A real answer replaces a stand-in only while the
            // quarter hasn't been ridden through: a quarter seen as «riverside» in the morning mustn't come back
            // as «gardens» in the evening – its spirit, requests and ingredients hang on its kind.
            // and a kind picked by hand is never overruled
            val seen = q.kind != null && (q.visits > 0 || q.changes > 0)
            if (real != null && seen) edit { s -> s.quarters[id]?.let { s.copy(quarters = s.quarters + (id to it.copy(guessed = false))) } ?: s }
            else if (real != null || q.kind == null) edit { s -> s.quarters[id]?.let { s.copy(quarters = s.quarters + (id to it.copy(kind = a, second = b, guessed = real == null))) } ?: s }
            tried[id] = System.currentTimeMillis()
            classifying.remove(id)
        }
    }

    private suspend fun enter(id: String) {
        val q = now.quarters[id] ?: return
        val fresh = q.visits == 0
        if (q.kind == null || (q.guessed && System.currentTimeMillis() - (tried[id] ?: 0) > 3 * 3600_000L)) classify(id)
        edit { s ->
            val qq = s.quarters[id] ?: return@edit s
            var t = s.trip
            val counted = t != null && id in t.quarters
            val upd = if (counted) qq else qq.copy(visits = qq.visits + 1, firstDay = if (qq.firstDay == 0L) today() else qq.firstDay)
            if (t != null && t.quarters.lastOrNull() != id) t = t.copy(quarters = t.quarters + id, fresh = if (fresh && id !in t.fresh) t.fresh + id else t.fresh)
            // a resident's letter is delivered, or a far ride made, by riding in
            val s2 = s.copy(quarters = s.quarters + (id to upd), here = id, trip = t)
            s2.copy(folk = Folk.entered(s2, id))
        }
        if (now.trip != null) news.tryEmit(News(if (fresh) tr("Новые земли: ${q.ru}", "New lands: ${q.en}") else q.name, speak = fresh))
    }

    // ---------- the ride ----------

    suspend fun startTrip(ride: Ride) {
        val t = System.currentTimeMillis()
        // the metro's entrance is found by the trip service, snapped to a station: an old fix would be the street before
        val f = live.value.fix?.takeIf { t - it.time < 10 * 60_000L && ride != Ride.METRO }
        edit { s ->
            if (s.trip != null) return@edit s
            val b = ride.beast.id
            val hatched = b !in s.mounts
            s.copy(
                trip = Trip(start = t, ride = ride.id, quarters = listOfNotNull(s.here.takeIf { it.isNotEmpty() }),
                    firstLat = f?.lat ?: 0.0, firstLon = f?.lon ?: 0.0, pts = listOfNotNull(f?.let { it.lat to it.lon }),
                    notes = if (hatched) listOf("Из яйца вылупился ${ride.beast.ru.lowercase()}!|A ${ride.beast.en.lowercase()} hatched!") else emptyList()),
                ride = ride.id, beast = b, lastCell = "",
                mounts = if (hatched) s.mounts + (b to Mount(b, born = t)) else s.mounts,
            )
        }
        live.value = Live(fix = live.value.fix, stillSince = t)
    }

    private fun knots(s: State): Set<String> {
        if (knotCache.first != s.ley.size) knotCache = s.ley.size to Ley.knots(s.ley)
        return knotCache.second
    }

    /** A position while a trip is on (and also between trips, to know the current quarter). */
    suspend fun onFix(f: Fix) {
        val prev = live.value
        val raw = f.speed?.toDouble() ?: prev.fix?.let { p ->
            val dt = (f.time - p.time) / 1000.0
            if (dt > 0.5) Geo.distance(p.lat, p.lon, f.lat, f.lon) / dt else null
        } ?: prev.speed
        val speed = if (prev.fix == null) raw else prev.speed * 0.5 + raw * 0.5
        val moving = speed > 3.5
        // a rough position (a bus deep in the city, a train, a phone in a bag) can't count metres, but a big enough
        // jump from the last one still says the ride goes on, and keeps it from ending by itself
        val heard = System.currentTimeMillis()
        val stir = prev.fix?.let { p -> f.accuracy <= 500 && Geo.distance(p.lat, p.lon, f.lat, f.lon) > maxOf(150.0, (f.accuracy + p.accuracy).toDouble()) } == true
        live.value = prev.copy(fix = f, speed = speed, heardAt = heard, stirAt = if (stir) heard else prev.stirAt,
            movingSince = if (moving) (prev.movingSince.takeIf { it > 0 } ?: f.time) else 0,
            stillSince = if (speed < 1.2) (prev.stillSince.takeIf { it > 0 } ?: f.time) else 0)

        val s = now
        val q = Quarters.at(s.quarters.values, f.lat, f.lon)
        if (q == null && s.quarters.isNotEmpty() && f.accuracy < 300) {
            fetchAround(f.lat, f.lon)
            if (s.here.isNotEmpty()) edit { it.copy(here = "") }
        }
        val trip = s.trip
        // long empty stretches get quarters of their own, so a ride through woods and fields still crosses several
        if (trip != null && s.quarters.isNotEmpty() && (f.accuracy <= 60 || f.simulated) &&
            s.quarters.values.none { Geo.distance(it.lat, it.lon, f.lat, f.lon) < Quarters.GAP }) spawn(f.lat, f.lon)
        if (trip == null) {
            if (q != null && q.id != s.here) edit { it.copy(here = q.id) }
            ensureKnown()
            return
        }
        if (f.accuracy > 60 && !f.simulated) return
        var meters = 0.0
        var underground = 0
        var jam = 0
        if (trip.lastAt > 0) {
            val d = Geo.distance(trip.lastLat, trip.lastLon, f.lat, f.lon)
            val dt = (f.time - trip.lastAt) / 1000.0
            // a gap in the fixes and a jump: the way went under the ground. By metro any jump is, however quick
            if (if (Ride.of(trip.ride) == Ride.METRO) d > 300 else dt > 150 && d > 800) underground = 1
            // noise under 25 m (or twice the fix's own error) is left; a car can't jump faster than 60 m/s;
            // a phone lying still by the satellites' own word hasn't gone anywhere, however the dot drifts
            val lying = f.speed != null && f.speed < 0.5 && d < 150
            if (d >= maxOf(25.0, f.accuracy * 2.0) && !lying && (dt <= 0 || d / dt < 60)) meters = d
        }
        // standing still mid-ride in a car or a bus, between fixes; it turns into a jam only when the ride moves on
        val step = prev.fix?.let { (f.time - it.time) / 1000.0 } ?: 0.0
        if (speed < 1.5 && step in 0.0..60.0 && trip.meters > 300 && Ride.of(trip.ride) != Ride.METRO) jam = step.toInt()
        val moved = meters > 0 || trip.lastAt == 0L
        val fast = if (meters > 0 && speed > 13.9 && underground == 0) meters else 0.0
        edit { st ->
            val t = st.trip ?: return@edit st
            if (!moved && jam == 0 && !(moving && t.jamWait > 0)) return@edit st
            var cells = st.cells; var ley = st.ley; var deep = st.deep
            var newCells = 0; var knotsHit = 0
            var lastCell = st.lastCell
            val lat0 = if (st.homeLat != 0.0) st.homeLat else f.lat
            val lon0 = if (st.homeLon != 0.0) st.homeLon else f.lon
            if (moved) {
                val c = Ley.cell(lat0, lon0, f.lat, f.lon)
                if (c.key != lastCell) {
                    val ks = knots(st)
                    val from = lastCell.takeIf { it.isNotEmpty() }?.let { Ley.Cell.of(it) }
                    val far = from != null && (underground == 1 || maxOf(kotlin.math.abs(c.x - from.x), kotlin.math.abs(c.y - from.y)) > 6)
                    val path = if (from == null) listOf(c) else if (far) listOf(from, c) else Ley.walk(from, c)
                    if (far) deep = deep + (Ley.edge(from!!, c) to (deep[Ley.edge(from, c)] ?: 0) + 1)
                    path.forEachIndexed { i, cc ->
                        if (i == 0 && from != null) return@forEachIndexed
                        val n = cells[cc.key] ?: 0
                        if (n == 0) newCells++
                        if (cc.key in ks) knotsHit++
                        cells = cells + (cc.key to n + 1)
                        if (i > 0 && !far) ley = ley + (Ley.edge(path[i - 1], cc) to (ley[Ley.edge(path[i - 1], cc)] ?: 0) + 1)
                    }
                    lastCell = c.key
                }
            }
            val lastPt = t.pts.lastOrNull()
            val addPt = moved && (lastPt == null || Geo.distance(lastPt.first, lastPt.second, f.lat, f.lon) >= 100)
            st.copy(cells = cells, ley = ley, deep = deep, lastCell = lastCell, trip = t.copy(
                meters = t.meters + meters, fastMeters = t.fastMeters + fast, // moving again: the wait was a jam if it was short; twenty minutes and more was a stop, not traffic
                jamSec = t.jamSec + if (moving && t.jamWait <= JAM_MAX) t.jamWait else 0, jamWait = if (moving) 0 else t.jamWait + jam,
                underground = t.underground + underground,
                newCells = t.newCells + newCells, knots = t.knots + knotsHit,
                night = t.night || Sky.dark(f.time, f.lat, f.lon), twilight = t.twilight || Sky.twilight(f.time, f.lat, f.lon),
                fullMoon = t.fullMoon || (Sky.fullMoon(f.time) && Sky.dark(f.time, f.lat, f.lon)),
                firstLat = if (t.firstLat == 0.0) f.lat else t.firstLat, firstLon = if (t.firstLon == 0.0) f.lon else t.firstLon,
                pts = if (addPt) (t.pts + (f.lat to f.lon)).takeLast(1500) else t.pts,
                lastLat = if (moved) f.lat else t.lastLat, lastLon = if (moved) f.lon else t.lastLon, lastAt = if (moved) f.time else t.lastAt,
            ))
        }
        if (q != null && trip.quarters.lastOrNull() != q.id) enter(q.id)
    }

    private val JAM_MAX = 20 * 60

    fun idle(nowMs: Long = System.currentTimeMillis()): Boolean {
        val t = now.trip ?: return false
        val l = live.value
        return Idle.over(nowMs, t.start, t.lastAt, l.stirAt, l.heardAt, Ride.of(t.ride) == Ride.METRO)
    }

    // ---------- the metro: stations instead of the GPS ----------

    /** One load at a time: a second asker (the picker while the ride's own load is under way) waits for it. */
    private val stationsLock = Mutex()

    /** How loading the stations went: loaded (the list may be empty – no metro), nowhere to load around, no answer. */
    enum class StationsLoad { OK, NO_PLACE, NO_NET }

    /**
     * The city's stations, loaded once around home (again after a month, or a day when none were found). Without
     * a home: around the phone, or [lat], [lon], or where the ride began.
     */
    suspend fun stations(lat: Double? = null, lon: Double? = null): StationsLoad = stationsLock.withLock {
        val s = now
        val t = System.currentTimeMillis()
        if (s.stationsAt > 0 && t - s.stationsAt < 30 * 86_400_000L) return@withLock StationsLoad.OK
        val at = (s.homeLat to s.homeLon).takeIf { it.first != 0.0 }
            ?: live.value.fix?.let { it.lat to it.lon }
            ?: (if (lat != null && lon != null) lat to lon else null)
            ?: s.trip?.takeIf { it.lastAt > 0 }?.let { it.firstLat to it.firstLon }
            ?: return@withLock StationsLoad.NO_PLACE
        val got = Metro.around(at.first, at.second) ?: return@withLock StationsLoad.NO_NET
        edit { it.copy(stations = got, stationsAt = if (got.isEmpty()) t - 29 * 86_400_000L else t) }
        StationsLoad.OK
    }

    /** The station a metro ride began at, when known. */
    fun metroStart(): Station? = now.trip?.takeIf { it.lastAt > 0 }?.let { Metro.nearest(now.stations, it.firstLat, it.firstLon, 50.0) }

    /** A position near a station becomes the station itself. */
    private fun snap(lat: Double, lon: Double): Pair<Double, Double> =
        Metro.nearest(now.stations, lat, lon)?.let { it.lat to it.lon } ?: (lat to lon)

    /**
     * Where a metro ride began: a station picked by hand, or the position found at the entrance ([auto]; it never
     * overrides a start already known).
     */
    suspend fun metroFrom(lat: Double, lon: Double, auto: Boolean = false) {
        val t = now.trip ?: return
        if (Ride.of(t.ride) != Ride.METRO) return
        val (la, lo) = snap(lat, lon)
        if (t.lastAt == 0L) {
            // timed at the start, so the way to the exit is never too fast to count
            onFix(Fix(la, lo, 5f, t.start, speed = 0f, simulated = true))
            return
        }
        if (auto) return
        // a different station than the one found: the ride starts there instead
        edit { s ->
            val tt = s.trip ?: return@edit s
            val lat0 = if (s.homeLat != 0.0) s.homeLat else la
            val lon0 = if (s.homeLon != 0.0) s.homeLon else lo
            val c = Ley.cell(lat0, lon0, la, lo)
            s.copy(lastCell = c.key, cells = if (c.key == s.lastCell) s.cells else s.cells + (c.key to (s.cells[c.key] ?: 0) + 1),
                trip = tt.copy(firstLat = la, firstLon = lo, lastLat = la, lastLon = lo, pts = listOf(la to lo)))
        }
        Quarters.at(now.quarters.values, la, lo)?.let { q -> if (now.trip?.quarters?.lastOrNull() != q.id) enter(q.id) }
    }

    /** The exit of a metro ride: one position (or a station picked), then the ride is over. */
    suspend fun metroEnd(lat: Double, lon: Double): Trip? {
        val (la, lo) = snap(lat, lon)
        if (now.trip != null) onFix(Fix(la, lo, 5f, System.currentTimeMillis(), speed = 0f, simulated = true))
        return endTrip()
    }

    /** A driver in motion sees only the road. */
    fun locked(): Boolean = now.trip?.let { Ride.of(it.ride).driver } == true && live.value.moving

    /** What the ride has gathered so far, before the map is read (shown on the way). */
    fun soFar(t: Trip): Map<String, Int> = baseGains(now, t, Weather.Now("", null))

    private fun baseGains(s: State, t: Trip, wx: Weather.Now): Map<String, Int> {
        val g = HashMap<String, Int>()
        fun add(k: String, n: Int) { if (n > 0) g[k] = (g[k] ?: 0) + n }
        val ride = Ride.of(t.ride)
        add("wind", (t.fastMeters / 2000).toInt().coerceAtMost(4))
        if (ride != Ride.METRO && ride != Ride.BIKE) add("stone", (t.jamSec / 240).coerceAtMost(4))
        if (t.night) add("moon", ((1 + t.km / 5).toInt().coerceAtMost(3)) * (if (t.fullMoon) 2 else 1))
        if (t.twilight) add("dawn", 2)
        when (wx.sky) { "rain" -> add("storm", 2); "snow" -> add("frost", 2); "fog" -> add("mist", 1); "clear" -> if (!t.night) add("sun", 2) }
        // where it hardly ever rains or snows: the wind brings storm water, a cold night leaves hoarfrost
        if (wx.windy && wx.sky != "rain") add("storm", 1)
        if (wx.temp != null && wx.sky != "snow" && (wx.temp < 0 || (t.night && wx.temp <= Weather.COLD_NIGHT))) add("frost", 1)
        if (ride == Ride.METRO || t.underground > 0) add("ore", (2 + t.km / 5).toInt().coerceAtMost(4))
        // cities without an underground: the forges' furnaces after dark give a little deep ore too
        else if (t.night && t.quarters.any { s.quarters[it]?.kind == Kind.FORGE }) add("ore", if (t.km >= 5) 2 else 1)
        add("shard", (t.newCells / 15).coerceAtMost(3))
        if (t.seeker) add("shard", 2)
        return g
    }

    /** Doubles and extras: the omen, the harvest elixir, the beast's traits. */
    private fun boost(s: State, t: Trip, raw: Map<String, Int>): Map<String, Int> {
        val g = raw.toMutableMap()
        val omen = Omen.of(dayOf(t.start))
        g[omen]?.let { g[omen] = it * 2 }
        val m = s.mounts[t.beast.id]
        if (m != null) for (tr in Mounts.traits) if (tr.earned(m)) {
            val hit = when (tr.id) { "rain" -> g.containsKey("storm") || g.containsKey("frost"); else -> g.containsKey(tr.ing) }
            if (hit) { val k = if (tr.id == "rain" && !g.containsKey("storm")) "frost" else tr.ing; g[k] = (g[k] ?: 0) + 1 }
        }
        if (t.harvest) g.keys.toList().forEach { g[it] = g.getValue(it) * 2 }
        return g
    }

    private fun discover(s: State, t: Trip): Pair<State, Trip> {
        // recipes are found by experiment (Lab); the road only shows what exists
        return s.copy(seen = s.seen + t.gains.keys) to t
    }

    private fun merge(a: Map<String, Int>, b: Map<String, Int>) = (a.keys + b.keys).associateWith { (a[it] ?: 0) + (b[it] ?: 0) }

    /** «Arrived»: the card is written, the beast grows, the sanctums count a visit; the map is asked afterwards. */
    suspend fun endTrip(): Trip? {
        var done: Trip? = null
        val cur = now.trip
        val wx = cur?.let { if (it.lastAt > 0) Weather.now(it.lastLat, it.lastLon) else Weather.Now("", null) } ?: Weather.Now("", null)
        edit { s0 ->
            val t0 = s0.trip ?: return@edit s0
            var s = s0
            val end = if (t0.lastAt > 0) maxOf(t0.lastAt, t0.start + 1000) else System.currentTimeMillis()
            var t = t0.copy(end = end, weather = wx.sky, temp = wx.temp ?: Double.NaN,
                harvest = (s.buffs["harvest"] ?: 0) > 0, seeker = (s.buffs["seeker"] ?: 0) > 0)
            val keep = t.km >= 0.3 || t.quarters.distinct().size > 1
            if (!keep) return@edit s.copy(trip = null, lastCell = "")
            val notes = t.notes.toMutableList()
            // what the ride itself gave
            val extra = HashMap<String, Int>()
            // sanctums at both ends; a warded one gives its ingredient
            var sanctums = s.sanctums
            var nid = s.nextId
            var startS = 0L; var endS = 0L
            if (t.firstLat != 0.0) { val (l, x) = Sanctums.visit(sanctums, t.firstLat, t.firstLon, nid, t.start); sanctums = l; if (x.id == nid) nid++; startS = x.id }
            if (t.lastAt > 0) { val (l, x) = Sanctums.visit(sanctums, t.lastLat, t.lastLon, nid, end); sanctums = l; if (x.id == nid) nid++; endS = x.id }
            for (id in setOf(startS, endS)) sanctums.firstOrNull { it.id == id && it.ward.isNotEmpty() }?.let { sc ->
                val ing = wardIng(sc.ward); extra[ing] = (extra[ing] ?: 0) + sc.wardLevel + if (Pages.complete(s, Cat.WARD)) 1 else 0
            }
            sanctums.firstOrNull { it.id == endS && it.visits == Sanctums.AWAKE }?.let { notes += "Пробудилось новое святилище|A new sanctum has awakened" }
            // spirits that are friends add theirs for every quarter of their kind passed
            val kinds = t.quarters.distinct().mapNotNull { s.quarters[it]?.kind }.toSet()
            val friendly = Spirits.friendGift(s)
            for (k in kinds) if ((s.spirits[k.id] ?: -1) >= 4) { val ing = Book.of(k); extra[ing] = (extra[ing] ?: 0) + friendly }
            // the quarter that blooms this week gives its gift once a day
            Luck.bloom(s.quarters.values.filter { it.visits > 0 }.map { it.id }, dayOf(t.start))?.let { (q, ing) ->
                if (q in t.quarters && s.bloomDay != dayOf(t.start)) {
                    extra[ing] = (extra[ing] ?: 0) + 3
                    s = s.copy(bloomDay = dayOf(t.start))
                    notes += "Цветущий квартал подарил: ${Book.ing(ing).ru.lowercase()} ×3|The blooming quarter gave ${Book.ing(ing).en.lowercase()} ×3"
                }
            }
            // surprises of the road
            var bonusPower = 0
            var golden = false
            if (t.km >= 1.5) {
                val r = Rand(hash("luck:${t.start}"))
                val roll = r.next()
                when {
                    roll < 0.05f -> { golden = true; notes += "Золотая жила! Дорога отдала всё вдвое|A golden vein! The road gave everything twice" }
                    roll < 0.22f -> {
                        val pool = Book.ingredients.map { it.id }
                        val ing = (pool.filter { it !in s.seen }.ifEmpty { pool }).let { it[r.int(it.size)] }
                        extra[ing] = (extra[ing] ?: 0) + 2
                        notes += "Блуждающий огонёк оставил подарок: ${Book.ing(ing).ru.lowercase()} ×2|A wandering wisp left a gift: ${Book.ing(ing).en.lowercase()} ×2"
                    }
                    roll < 0.34f -> { bonusPower = 30 + r.int(40); notes += "${t.beast.ru} учуял клад: +$bonusPower силы|Your ${t.beast.en.lowercase()} sniffed out a hoard: +$bonusPower power" }
                    roll < 0.42f && t.night -> { extra["shard"] = (extra["shard"] ?: 0) + 1; notes += "Звездопад! Один осколок упал прямо на дорогу|A meteor shower! One shard fell right onto the road" }
                }
                if (t.fullMoon && r.next() < 0.5f) { extra["shard"] = (extra["shard"] ?: 0) + 3; extra["moon"] = (extra["moon"] ?: 0) + 2; notes += "Над городом пронеслась Дикая охота и обронила звёзды|The Wild Hunt swept over the city and dropped some stars" }
            }
            var gains = merge(boost(s, t, baseGains(s, t, wx)), extra)
            if (golden) gains = gains.mapValues { it.value * 2 }
            // power: ten a kilometre, knots and new streets add, the tailwind doubles
            var power = (t.km * 10).toInt() + (t.knots * 2).coerceAtMost(20) + t.newCells
            if ((s.buffs["tailwind"] ?: 0) > 0) power *= 2
            power = (power * Decors.bonus(s.decor.size)).toInt() + bonusPower
            val xp = t.km * (if ((s.buffs["longroad"] ?: 0) > 0) 1.5 else 1.0) * (if (s.great) 1.1 else 1.0) * (if (Pages.complete(s, Cat.AWAKEN)) 1.1 else 1.0)
            // the beast
            val b = t.beast.id
            val m0 = s.mounts[b] ?: Mount(b, born = t.start)
            val wet = wx.sky == "rain" || wx.sky == "snow" || wx.windy
            val m = m0.copy(km = m0.km + xp, rides = m0.rides + 1, nightKm = m0.nightKm + if (t.night) t.km else 0.0,
                wetRides = m0.wetRides + if (wet) 1 else 0, fastKm = m0.fastKm + t.fastMeters / 1000, jamMin = m0.jamMin + t.jamSec / 60,
                newCells = m0.newCells + t.newCells, lastRide = end)
            if (!Mounts.ready(m0) && Mounts.ready(m)) notes += "${m.kind.ru} готов к пробуждению|Your ${m.kind.en.lowercase()} is ready to awaken"
            Mounts.traits.firstOrNull { !it.earned(m0) && it.earned(m) }?.let { notes += "Новая черта: ${it.ru}|A new trait: ${it.en}" }
            // spirits met for the first time
            val met = kinds.filter { it.id !in s.spirits }.map { it.id }
            val spirits = s.spirits + met.associateWith { 0 }
            val since = s.spiritSince + met.associateWith { end }
            // elixirs wear off
            val buffs = s.buffs.mapValues { (k, v) -> if (k in setOf("tailwind", "longroad", "harvest", "seeker")) v - 1 else v }.filterValues { it > 0 }
            t = t.copy(gains = gains, power = power, xp = xp, met = met, notes = notes, startSanctum = startS, endSanctum = endS)
            s = s.copy(bag = merge(s.bag, gains), power = s.power + power, mounts = s.mounts + (b to m), sanctums = sanctums, nextId = nid,
                spirits = spirits, spiritSince = since, buffs = buffs)
            val (s2, t2) = discover(s, t)
            done = t2
            s2.copy(trip = null, lastCell = "", trips = (listOf(t2) + s2.trips).take(400).mapIndexed { i, x -> if (i >= 150 && x.pts.isNotEmpty()) x.copy(pts = emptyList()) else x })
        }
        val t = done
        if (t != null) { finished.value = t.start; enrich(t.start) }
        live.value = Live(fix = live.value.fix)
        return t
    }

    private fun wardIng(w: String) = when (w) {
        "w_grove" -> "resin"; "w_well" -> "mist"; "w_lantern" -> "moon"; "w_menhir" -> "stone"; "w_bell" -> "wax"; else -> "shard"
    }

    /** Asks the map what the ride passed and adds it; then the spirits' requests are checked. */
    fun enrich(start: Long) {
        if (!asking.add(start)) return
        scope.launch {
            val t0 = now.trips.firstOrNull { it.start == start }
            if (t0 == null || t0.osm == 1) { asking.remove(start); return@launch }
            val got = if (t0.pts.size >= 2) Lore.along(t0.pts) else emptyMap()
            edit { s0 ->
                val i = s0.trips.indexOfFirst { it.start == start }
                if (i < 0) return@edit s0
                var t = s0.trips[i]
                if (t.osm == 1) return@edit s0
                var s = s0
                val first = t.osm == 0
                if (got != null) {
                    val g = boost(s, t.copy(harvest = t.harvest), got).filterKeys { it in got.keys || it == "mist" }
                    t = t.copy(gains = merge(t.gains, g), osm = 1)
                    s = s.copy(bag = merge(s.bag, g))
                    if ((g["mist"] ?: 0) > 0) s.mounts[t.beast.id]?.let { m -> s = s.copy(mounts = s.mounts + (m.beast to m.copy(rivers = m.rivers + 1))) }
                    val (s2, t2) = discover(s, t); s = s2; t = t2
                } else t = t.copy(osm = 2)
                // quarters whose kind was learnt only after the stop: their spirits notice you now
                val late = t.quarters.distinct().mapNotNull { s.quarters[it]?.kind }.filter { it.id !in s.spirits }.map { it.id }.distinct()
                if (late.isNotEmpty()) {
                    s = s.copy(spirits = s.spirits + late.associateWith { 0 }, spiritSince = s.spiritSince + late.associateWith { t.end })
                    t = t.copy(met = (t.met + late).distinct())
                }
                // requests are judged once, at the first answer (or the first failure)
                if (first) { val (s3, t3) = judge(s, t); s = s3; t = t3 }
                s.copy(trips = s.trips.toMutableList().also { it[i] = t })
            }
            asking.remove(start)
        }
    }

    /** Moves the spirits' PASS and RIDE requests on after a ride. */
    private fun judge(s0: State, t: Trip): Pair<State, Trip> {
        var s = s0
        val kinds = t.quarters.distinct().mapNotNull { s.quarters[it]?.kind }.toSet()
        val notes = t.notes.toMutableList()
        val top = s.sanctums.maxByOrNull { it.visits }?.id
        for ((id, step) in s0.spirits) {
            if (step >= 4 || id in t.met) continue
            val sp = Spirits.byId[id] ?: continue
            val st = sp.steps[step]
            val inKind = sp.kind in kinds
            val ok = when (st.ask) {
                Ask.PASS -> inKind
                Ask.RIDE -> when (st.what) {
                    "dawn" -> inKind && t.twilight
                    "bridge" -> inKind && (t.gains["mist"] ?: 0) > 0
                    "night" -> inKind && t.night
                    "ink" -> inKind && (t.gains["ink"] ?: 0) > 0
                    "spark" -> inKind && (t.gains["spark"] ?: 0) > 0
                    "weekend" -> inKind && Instant.ofEpochMilli(t.start).atZone(zone).dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                    "long" -> t.km >= 10
                    else -> t.endSanctum != 0L && t.endSanctum == top
                }
                else -> false
            }
            if (!ok) continue
            val n = (s.spiritCount[id] ?: 0) + 1
            s = s.copy(spiritCount = s.spiritCount + (id to n))
            if (n == st.n) notes += "${sp.ru} доволен – загляните к нему|${sp.en} is pleased – go and see"
        }
        return s to t.copy(notes = notes)
    }

    // ---------- the cauldron ----------

    fun canBrew(s: State, id: String): Boolean {
        val r = Book.recipe[id] ?: return false
        return id in s.known && s.power >= r.power && r.needs.all { (k, n) -> s.has(k) >= n }
    }

    suspend fun brew(id: String): Boolean {
        var ok = false
        val r = Book.recipe[id] ?: return false
        edit { s ->
            if (!canBrew(s, id)) return@edit s
            ok = true
            val bag = s.bag.toMutableMap()
            r.needs.forEach { (k, n) -> bag[k] = (bag[k] ?: 0) - n }
            var n = s.copy(bag = bag.filterValues { it > 0 }, power = s.power - r.power, brewed = s.brewed + (id to (s.brewed[id] ?: 0) + 1))
            n = when (id) {
                "aurora" -> n.copy(aurora = true)
                "mirror" -> n.copy(mirror = true)
                "great" -> n.copy(great = true)
                else -> n.copy(stock = n.stock + (id to (n.stock[id] ?: 0) + 1))
            }
            n
        }
        if (ok) news.tryEmit(News(tr("Сварено: ${r.ru}", "Brewed: ${r.en}")))
        return ok
    }

    /** Distils one of the sky's ingredients ([Book.WEATHER]) from [Book.DISTIL] of the one there is most of. */
    suspend fun distil(target: String): Boolean {
        var from: String? = null
        edit { s ->
            val src = Book.distilFrom(s.bag, target) ?: return@edit s
            from = src
            val bag = s.bag.toMutableMap()
            bag[src] = (bag[src] ?: 0) - Book.DISTIL
            bag[target] = (bag[target] ?: 0) + 1
            s.copy(bag = bag.filterValues { it > 0 }, seen = s.seen + target)
        }
        from?.let { news.tryEmit(News(tr("Перегнано: ${Book.ing(it).ru} → ${Book.ing(target).ru}", "Distilled: ${Book.ing(it).en} → ${Book.ing(target).en}"))) }
        return from != null
    }

    /** What an experiment came to. */
    class Trial(val recipe: Recipe? = null, val dud: String = "", val hint: String = "", val again: Boolean = false)

    /**
     * Throws one of each of [ids] (two or three different ingredients) into the cauldron. A right set writes its
     * recipe into the grimoire; a wrong one is spent on a dud with a hint. Sets already tried cost nothing.
     */
    suspend fun experiment(ids: List<String>): Trial? {
        val set = ids.distinct()
        if (set.size !in 2..3) return null
        var out: Trial? = null
        edit { s ->
            if (set.any { s.has(it) < 1 }) return@edit s
            val key = Lab.key(set)
            val r = Lab.match(set)
            if (r != null && r.id in s.known) { out = Trial(r, again = true); return@edit s }
            if (key in s.tried) { out = Trial(dud = Lab.dud(key), hint = Lab.hint(set, s.known), again = true); return@edit s }
            val bag = s.bag.toMutableMap()
            set.forEach { bag[it] = (bag[it] ?: 0) - 1 }
            val left = bag.filterValues { it > 0 }
            if (r != null) {
                var known = s.known + r.id
                if (Book.recipes.all { it.id == "great" || it.id in known }) known = known + "great"
                out = Trial(r)
                s.copy(bag = left, known = known)
            } else {
                out = Trial(dud = Lab.dud(key), hint = Lab.hint(set, s.known))
                s.copy(bag = left, tried = s.tried + key, power = s.power + Lab.DUD_POWER)
            }
        }
        return out
    }

    fun canBuy(s: State, d: Decor) = d.id !in s.decor && s.power >= d.power && d.needs.all { (k, n) -> s.has(k) >= n }

    fun buy(id: String) = write { s ->
        val d = Decors.byId[id] ?: return@write s
        if (!canBuy(s, d)) return@write s
        val bag = s.bag.toMutableMap()
        d.needs.forEach { (k, n) -> bag[k] = (bag[k] ?: 0) - n }
        news.tryEmit(News(tr("В башне стало уютнее: ${d.ru.lowercase()}", "The tower is cozier: ${d.en.lowercase()}")))
        s.copy(bag = bag.filterValues { it > 0 }, power = s.power - d.power, decor = s.decor + id)
    }

    /** Drinks an elixir: it works on the next rides. */
    fun drink(id: String) = write { s ->
        val have = s.stock[id] ?: 0
        val r = Book.recipe[id]
        if (have <= 0 || r?.cat != Cat.ELIXIR) return@write s
        val rides = (if (id == "tailwind" || id == "longroad") 3 else 1) + (if (Pages.complete(s, Cat.ELIXIR)) 1 else 0)
        s.copy(stock = (s.stock + (id to have - 1)).filterValues { it > 0 }, buffs = s.buffs + (id to (s.buffs[id] ?: 0) + rides))
    }

    fun awaken(beast: String) = write { s ->
        val m = s.mounts[beast] ?: return@write s
        if (!Mounts.ready(m)) return@write s
        val p = Mounts.AWAKEN[m.stage]
        val have = s.stock[p] ?: 0
        if (have <= 0) return@write s
        news.tryEmit(News(tr("${m.kind.ru} пробудился: теперь он ${Mounts.stageName(m.stage + 1)}", "Your ${m.kind.en.lowercase()} has awakened: now ${Mounts.stageName(m.stage + 1)}")))
        // an awakened beast remembers a little of the old lore: a clue to a recipe
        Clues.give(s.copy(stock = (s.stock + (p to have - 1)).filterValues { it > 0 }, mounts = s.mounts + (beast to m.copy(stage = m.stage + 1)))) { news.tryEmit(News(it)) }
    }

    /** Sets a ward on an awakened sanctum; the same ward again raises it (up to 3). */
    fun ward(sanctum: Long, ward: String) = write { s ->
        val sc = s.sanctums.firstOrNull { it.id == sanctum } ?: return@write s
        val have = s.stock[ward] ?: 0
        if (!sc.awake || have <= 0 || (sc.ward == ward && sc.wardLevel >= 3)) return@write s
        val upd = if (sc.ward == ward) sc.copy(wardLevel = sc.wardLevel + 1) else sc.copy(ward = ward, wardLevel = 1)
        s.copy(stock = (s.stock + (ward to have - 1)).filterValues { it > 0 }, sanctums = s.sanctums.map { if (it.id == sanctum) upd else it })
    }

    /** Lets a stray sanctum go out, with its ward; rides starting or ending there again begin a new one. */
    fun forget(sanctum: Long) = write { s -> s.copy(sanctums = s.sanctums.filter { it.id != sanctum }) }

    fun rename(sanctum: Long, name: String) = write { s -> s.copy(sanctums = s.sanctums.map { if (it.id == sanctum) it.copy(name = name.trim().take(30)) else it }) }

    fun renameBeast(beast: String, name: String) = write { s -> s.mounts[beast]?.let { s.copy(mounts = s.mounts + (beast to it.copy(name = name.trim().take(20)))) } ?: s }

    /**
     * You name the quarter's kind yourself: the map may call a whole district «gardens» for its lawns. Its second
     * kind stays unless it is the new one; the first change is free, the rest cost power.
     */
    suspend fun rekind(id: String, k: Kind): Boolean {
        var ok = false
        edit { s ->
            val q = s.quarters[id] ?: return@edit s
            val price = Quarters.rekindPrice(q)
            if (q.kind == null || q.kind == k || s.power < price) return@edit s
            ok = true
            val second = q.second?.takeIf { it != k } ?: q.kind
            s.copy(power = s.power - price, quarters = s.quarters + (id to q.copy(kind = k, second = second, guessed = false, changes = q.changes + 1)))
        }
        return ok
    }

    // ---------- the residents ----------

    /** Takes the resident's current request; a letter gets its quarter now (the one its words already name). */
    suspend fun takeErrand(quarter: String): Boolean {
        var ok = false
        edit { s ->
            val e = Folk.at(s, quarter) ?: return@edit s
            if (e.since > 0 || Folk.done(e) || Folk.taken(s) >= Folk.MAX_TAKEN) return@edit s
            var e2 = e.copy(since = System.currentTimeMillis(), arrived = false)
            if (Folk.step(e)?.ask == Folk.Ask.Letter) {
                val target = Folk.pickTarget(s, e)
                if (target.isEmpty()) return@edit s
                e2 = e2.copy(target = target)
            }
            ok = true
            s.copy(folk = s.folk.map { if (it.quarter == quarter) e2 else it })
        }
        return ok
    }

    /** Reports back: ingredients or the potion are handed over, the reward comes, the next request waits to be taken. */
    suspend fun claimErrand(quarter: String): Boolean {
        var ok = false
        edit { s ->
            val e = Folk.at(s, quarter) ?: return@edit s
            if (!Folk.ready(s, e)) return@edit s
            val i = e.step
            var bag = s.bag
            var stock = s.stock
            when (val a = Folk.step(e)?.ask) {
                is Folk.Ask.Give -> bag = (bag + (a.ing to s.has(a.ing) - a.n)).filterValues { it > 0 }
                is Folk.Ask.Potion -> stock = (stock + (a.recipe to (stock[a.recipe] ?: 0) - 1)).filterValues { it > 0 }
                else -> {}
            }
            val gift = Folk.gift(s, e)
            if (gift != null) bag = bag + (gift to (bag[gift] ?: 0) + Folk.gifts[i])
            ok = true
            val next = e.copy(step = i + 1, since = 0, arrived = false)
            val who = Folk.of(e)
            news.tryEmit(News(if (Folk.done(next)) tr("${who.ru}: «${who.thanksRu}»", "${who.en}: «${who.thanksEn}»")
                else tr("${who.ru} благодарит: +${Folk.power[i]} силы", "${who.en} thanks you: +${Folk.power[i]} power")))
            val s2 = s.copy(bag = bag, stock = stock, power = s.power + Folk.power[i], seen = s.seen + listOfNotNull(gift),
                folk = s.folk.map { if (it.quarter == quarter) next else it })
            // a finished story ends with a clue to a recipe
            if (Folk.done(next)) Clues.give(s2) { news.tryEmit(News(it)) } else s2
        }
        return ok
    }

    suspend fun reset() = edit { State(theme = it.theme, voice = it.voice) }

    // ---------- the spirits ----------

    /** True when the spirit's current request can be finished now. */
    fun spiritReady(s: State, id: String): Boolean {
        val step = s.spirits[id] ?: return false
        if (step >= 4) return false
        val sp = Spirits.byId[id] ?: return false
        val st = sp.steps[step]
        return when (st.ask) {
            Ask.PASS, Ask.RIDE -> (s.spiritCount[id] ?: 0) >= st.n
            Ask.GIVE -> s.has(st.what) >= st.n
            Ask.GIFT -> (s.stock[sp.gift] ?: 0) > 0
        }
    }

    fun spiritDone(id: String) = write { s0 ->
        if (!spiritReady(s0, id)) return@write s0
        val sp = Spirits.byId.getValue(id)
        val step = s0.spirits.getValue(id)
        val st = sp.steps[step]
        var s = s0
        when (st.ask) {
            Ask.GIVE -> s = s.copy(bag = (s.bag + (st.what to s.has(st.what) - st.n)).filterValues { it > 0 })
            Ask.GIFT -> s = s.copy(stock = (s.stock + (sp.gift to (s.stock[sp.gift] ?: 0) - 1)).filterValues { it > 0 })
            else -> {}
        }
        val (p, n) = Spirits.reward(step)
        news.tryEmit(News(if (step == 3) tr("${sp.ru} теперь ваш друг!", "${sp.en} is your friend now!") else tr("+$p силы и ${Book.ing(sp.ing).ru.lowercase()} ×$n", "+$p power and ${Book.ing(sp.ing).en.lowercase()} ×$n")))
        val bag = s.bag + (sp.ing to s.has(sp.ing) + n)
        val seen = s.seen + sp.ing
        // before the last request the spirit tells how its gift is made
        var known = if (step + 1 == 3) s.known + sp.gift else s.known
        if (Book.recipes.all { it.id == "great" || it.id in known }) known = known + "great"
        val done = s.copy(power = s.power + p, bag = bag, seen = seen, known = known, spirits = s.spirits + (id to step + 1), spiritCount = s.spiritCount + (id to 0),
            spiritSince = s.spiritSince + (id to System.currentTimeMillis()))
        // each of the first three requests ends with a clue to a recipe; the fourth makes a friend
        if (step < 3) Clues.give(done) { news.tryEmit(News(it)) } else done
    }
}

/**
 * When a ride ends by itself. Its end is the last place it moved to, so waiting longer costs nothing but a little
 * battery, while ending too early loses the rest of the ride (a long jam, a train, the GPS gone quiet).
 */
object Idle {
    /** The phone says the ride stands: a jam up to twenty minutes is still part of it (see the jams' own limit). */
    const val STILL = 20 * 60_000L
    /** No positions at all: the satellites are lost (a train, a tunnel, a bag), not the ride. */
    const val SILENT = 45 * 60_000L
    /** This long without any position is silence. */
    const val QUIET = 3 * 60_000L
    /** Under the ground there is nothing to tell standing from riding: a metro ride waits for «Arrived». */
    const val METRO = 4 * 3600_000L

    fun over(now: Long, start: Long, movedAt: Long, stirAt: Long, heardAt: Long, metro: Boolean): Boolean {
        val since = maxOf(if (movedAt > 0) movedAt else start, stirAt)
        val limit = when {
            metro -> METRO
            now - maxOf(heardAt, start) > QUIET -> SILENT
            else -> STILL
        }
        return now - since > limit
    }
}
