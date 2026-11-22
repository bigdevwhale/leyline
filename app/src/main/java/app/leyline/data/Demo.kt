package app.leyline.data

import android.content.Context
import app.leyline.trip.Fix
import org.json.JSONObject

/**
 * Screenshot builds (-Pdemo) start from a ready game (assets/demo.json, made by tools/demo.py).
 * Its times are written around a fixed «base» moment and moved to now here, so the demo never looks stale.
 */
object Demo {
    /** What the demo opens on: a tab and a sheet, from the capture script's intent. */
    @Volatile var tab = -1
    @Volatile var open: String? = null

    suspend fun seedIfEmpty(c: Context, g: Game) {
        if (g.now.onboarded) return
        val text = runCatching { c.assets.open("demo.json").bufferedReader().use { it.readText() } }.getOrNull() ?: return
        val o = JSONObject(text)
        val d = System.currentTimeMillis() - o.optLong("base", System.currentTimeMillis())
        val s = stateOf(o)
        fun t(x: Long) = if (x > 0) x + d else x
        val moved = s.copy(
            trips = s.trips.map { it.copy(start = t(it.start), end = t(it.end), lastAt = t(it.lastAt)) },
            mounts = s.mounts.mapValues { (_, m) -> m.copy(born = t(m.born), lastRide = t(m.lastRide)) },
            spiritSince = s.spiritSince.mapValues { t(it.value) },
            sanctums = s.sanctums.map { it.copy(since = t(it.since)) },
        )
        g.edit { moved }
        g.live.value = g.live.value.copy(fix = Fix(o.optDouble("fixLat", s.homeLat), o.optDouble("fixLon", s.homeLon), 8f, System.currentTimeMillis()))
    }
}
