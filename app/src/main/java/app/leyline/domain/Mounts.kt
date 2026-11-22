package app.leyline.domain

/** One beast of the stable and what it has been through. */
data class Mount(
    val beast: String,
    val km: Double = 0.0,
    val stage: Int = 0,
    val rides: Int = 0,
    val nightKm: Double = 0.0,
    val wetRides: Int = 0,
    val fastKm: Double = 0.0,
    val jamMin: Int = 0,
    val rivers: Int = 0,
    val newCells: Int = 0,
    val born: Long = 0,
    val lastRide: Long = 0,
    val name: String = "",
) {
    val kind get() = Beast.of(beast)
    val title get() = name.ifEmpty { kind.title }
}

/** A trait a beast earns by how it is ridden; each adds one ingredient on fitting rides. */
class Trait(val id: String, val ru: String, val en: String, val doesRu: String, val doesEn: String, val ing: String,
            val goal: Double, val have: (Mount) -> Double, val unitRu: String, val unitEn: String) {
    val name get() = tr(ru, en)
    val does get() = tr(doesRu, doesEn)
    fun earned(m: Mount) = have(m) >= goal
}

object Mounts {
    /** Kilometres to be ready for the next stage (and an awakening potion). */
    val NEED = listOf(25.0, 120.0, 500.0)
    val AWAKEN = listOf("awaken1", "awaken2", "awaken3")

    fun stageName(s: Int) = when (s) { 0 -> tr("детёныш", "baby"); 1 -> tr("юный", "young"); 2 -> tr("взрослый", "grown"); else -> tr("древний", "ancient") }

    /** True when the beast has ridden enough for its next awakening. */
    fun ready(m: Mount) = m.stage < 3 && m.km >= NEED[m.stage]

    /** Progress to the next stage, 0..1 (1 at the top). */
    fun progress(m: Mount): Float {
        if (m.stage >= 3) return 1f
        val from = if (m.stage == 0) 0.0 else NEED[m.stage - 1]
        return ((m.km - from) / (NEED[m.stage] - from)).toFloat().coerceIn(0f, 1f)
    }

    /** It rests when it hasn't been out for a week; nothing is lost. */
    fun asleep(m: Mount, now: Long = System.currentTimeMillis()) = m.lastRide > 0 && now - m.lastRide > 7 * 86_400_000L

    val traits = listOf(
        Trait("night", "Ночной глаз", "Night eye", "+1 лунная пыль в ночных поездках", "+1 moon dust on night rides", "moon", 15.0, { it.nightKm }, "км ночью", "km at night"),
        Trait("rain", "Дождевая чешуя", "Rain scales", "+1 грозовая вода или иней в непогоду", "+1 storm water or hoarfrost in bad weather", "storm", 4.0, { it.wetRides.toDouble() }, "поездки в непогоду", "rides in bad weather"),
        Trait("wind", "Ветрокрыл", "Windwing", "+1 ветер, когда дорога быстрая", "+1 wind when the road is fast", "wind", 40.0, { it.fastKm }, "быстрых км", "fast km"),
        Trait("stone", "Каменное сердце", "Stone heart", "+1 камень терпения в пробках", "+1 patience stone in jams", "stone", 60.0, { it.jamMin.toDouble() }, "минут в пробках", "minutes in jams"),
        Trait("river", "Речной нюх", "River nose", "+1 речной туман у воды", "+1 river mist by the water", "mist", 8.0, { it.rivers.toDouble() }, "встреч с рекой", "river crossings"),
        Trait("scout", "Следопыт", "Pathfinder", "+1 звёздный осколок в новых местах", "+1 star shard in new places", "shard", 40.0, { it.newCells.toDouble() }, "новых мест", "new places"),
    )
}
