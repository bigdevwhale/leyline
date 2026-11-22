package app.leyline.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** The sun and the moon over the city: when it gets dark, whether it's dawn, how full the moon is. */
object Sky {
    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI

    /** Sunrise and sunset (ms) on [day] at [lat],[lon]; null in polar day or night. */
    fun sun(day: Long, lat: Double, lon: Double): Pair<Long, Long>? {
        // NOAA's simple algorithm, good to a minute or two
        val n = LocalDate.ofEpochDay(day).dayOfYear
        val g = 2 * PI / 365 * (n - 1)
        val eq = 229.18 * (0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g))
        val decl = 0.006918 - 0.399912 * cos(g) + 0.070257 * sin(g) - 0.006758 * cos(2 * g) + 0.000907 * sin(2 * g) - 0.002697 * cos(3 * g) + 0.00148 * sin(3 * g)
        val c = cos(rad(90.833)) / (cos(rad(lat)) * cos(decl)) - tan(rad(lat)) * tan(decl)
        if (c < -1 || c > 1) return null
        val ha = deg(acos(c))
        val noon = 720 - 4 * lon - eq
        val midnight = LocalDate.ofEpochDay(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return (midnight + ((noon - 4 * ha) * 60_000).toLong()) to (midnight + ((noon + 4 * ha) * 60_000).toLong())
    }

    /** True when the sun is down at [ms]. Without a place, by the clock: 21:00–5:00. */
    fun dark(ms: Long, lat: Double, lon: Double): Boolean {
        if (lat == 0.0 && lon == 0.0) { val h = hourOf(ms); return h >= 21 || h < 5 }
        val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
        val s = sun(day, lat, lon) ?: return abs(lat) > 60 && LocalDate.ofEpochDay(day).monthValue in listOf(11, 12, 1)
        return ms < s.first || ms > s.second
    }

    /** Within 45 minutes of sunrise or sunset. */
    fun twilight(ms: Long, lat: Double, lon: Double): Boolean {
        if (lat == 0.0 && lon == 0.0) return false
        val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
        val s = sun(day, lat, lon) ?: return false
        val w = 45 * 60_000L
        return kotlin.math.abs(ms - s.first) < w || kotlin.math.abs(ms - s.second) < w
    }

    /** The moon's age in days since the new moon, 0 ≤ age < 29.53. */
    fun moonAge(ms: Long): Double {
        val ref = 947_182_440_000L // new moon 2000-01-06 18:14 UTC
        val p = 29.530588853
        val d = (ms - ref) / 86_400_000.0
        return d - floor(d / p) * p
    }

    fun fullMoon(ms: Long) = moonAge(ms) in 13.3..16.3
    fun newMoon(ms: Long) = moonAge(ms).let { it < 1.5 || it > 28.0 }

    /** 0..7 for the phase pictures: new, waxing crescent, first quarter, waxing gibbous, full, … */
    fun phase(ms: Long) = ((moonAge(ms) / 29.530588853 * 8 + 0.5).toInt()) % 8

    fun phaseName(ms: Long) = when (phase(ms)) {
        0 -> tr("новолуние", "new moon"); 1 -> tr("молодой месяц", "waxing crescent"); 2 -> tr("первая четверть", "first quarter")
        3 -> tr("растущая луна", "waxing gibbous"); 4 -> tr("полнолуние", "full moon"); 5 -> tr("убывающая луна", "waning gibbous")
        6 -> tr("последняя четверть", "last quarter"); else -> tr("старый месяц", "waning crescent")
    }

    private fun abs(v: Double) = kotlin.math.abs(v)
    @Suppress("unused") private fun asinD(v: Double) = deg(asin(v))
}

/** The omen of the day: one ingredient the city is full of today (it comes twice as often). */
object Omen {
    private val POOL = listOf("wind", "stone", "mist", "resin", "moon", "wax", "dust", "ink", "spark", "feather", "lily", "copper", "dawn", "shard")

    fun of(day: Long = today()): String = POOL[Rand(hash("omen:$day")).int(POOL.size)]

    fun line(day: Long = today()): String = when (of(day)) {
        "wind" -> tr("Сегодня по улицам гуляет ветер", "Today the wind roams the streets")
        "stone" -> tr("Сегодня камни терпеливы как никогда", "Today the stones are patient as never before")
        "mist" -> tr("Сегодня реки дышат туманом", "Today the rivers breathe mist")
        "resin" -> tr("Сегодня в парках плачут сосны", "Today the pines weep in the parks")
        "moon" -> tr("Сегодня луна щедра на пыль", "Today the moon is generous with dust")
        "wax" -> tr("Сегодня во всех храмах горят свечи", "Today candles burn in every church")
        "dust" -> tr("Сегодня старые стены вспоминают былое", "Today the old walls remember the past")
        "ink" -> tr("Сегодня мудрецы пишут без устали", "Today the sages write without rest")
        "spark" -> tr("Сегодня в кузнях пляшут искры", "Today sparks dance in the forges")
        "feather" -> tr("Сегодня странники в пути", "Today the wanderers are on the road")
        "lily" -> tr("Сегодня тихие места цветут", "Today the quiet places bloom")
        "copper" -> tr("Сегодня на торжищах звенит медь", "Today copper rings in the markets")
        "dawn" -> tr("Сегодня заря щедра на росу", "Today the dawn is generous with dew")
        else -> tr("Сегодня падают звёзды", "Today the stars are falling")
    }
}
