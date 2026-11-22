package app.leyline.domain

/**
 * What a quarter of the city is, read from what OpenStreetMap has in it. Each kind has its guardian spirit
 * (spirit_<id>) and an ingredient it is rich in.
 */
enum class Kind(val id: String, val ru: String, val en: String) {
    GARDEN("garden", "Сады", "Gardens"),
    WATER("water", "Набережная", "Riverside"),
    OLD("old", "Старый город", "Old town"),
    SCHOLAR("scholar", "Учёный квартал", "Scholars"),
    FORGE("forge", "Кузни", "Forges"),
    BAZAAR("bazaar", "Торжище", "Market"),
    GATE("gate", "Ворота", "Gates"),
    HOMES("homes", "Слобода", "Homesteads");

    val title get() = tr(ru, en)

    /** The second kind a quarter is guessed as, when the map server is silent. */
    val near: List<Kind> get() = when (this) {
        GARDEN -> listOf(HOMES, WATER); WATER -> listOf(GARDEN, OLD); OLD -> listOf(SCHOLAR, BAZAAR); SCHOLAR -> listOf(OLD, GATE)
        FORGE -> listOf(GATE, HOMES); BAZAAR -> listOf(OLD, HOMES); GATE -> listOf(BAZAAR, FORGE); HOMES -> listOf(GARDEN, BAZAAR)
    }

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
}

/**
 * The creatures that carry you. Each way of riding has its own; it hatches on the first ride that way.
 * [pic] is the sheet prefix: mount_<id>_<stage>.
 */
enum class Beast(val id: String, val ru: String, val en: String, val kindRu: String, val kindEn: String) {
    DRAKE("car", "Дрейк", "Drake", "дорожный дракон", "a road dragon"),
    BEETLE("bus", "Жук-тягач", "Carrier beetle", "многоногий жук с фонариками", "a many-legged beetle with lanterns"),
    SERPENT("tram", "Искрозмей", "Spark serpent", "змей, что скользит по проводам", "a serpent that glides along the wires"),
    WYRM("metro", "Червь глубин", "Burrow wyrm", "подземный змей-крот", "an underground mole-dragon"),
    FOX("bike", "Лис-скороход", "Wind fox", "двухвостый лис ветров", "a two-tailed wind fox");

    val title get() = tr(ru, en)
    val what get() = tr(kindRu, kindEn)
    fun pic(stage: Int) = "mount_${id}_" + listOf("baby", "young", "grown", "elder")[stage.coerceIn(0, 3)]
    val sleepPic get() = "mount_${id}_sleep"
    val joyPic get() = "mount_${id}_joy"

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: DRAKE }
}

/** How you ride this time. Only drivers (car, bike) are kept off the screen while moving. */
enum class Ride(val id: String, val ru: String, val en: String, val driver: Boolean, val beast: Beast) {
    CAR("car", "За рулём", "Driving", true, Beast.DRAKE),
    TAXI("taxi", "Пассажиром", "As a passenger", false, Beast.DRAKE),
    BUS("bus", "Автобус", "Bus", false, Beast.BEETLE),
    TRAM("tram", "Трамвай, электричка", "Tram, train", false, Beast.SERPENT),
    METRO("metro", "Метро", "Underground", false, Beast.WYRM),
    BIKE("bike", "Велосипед, самокат", "Bike, scooter", true, Beast.FOX);

    val title get() = tr(ru, en)
    val sub get() = when (this) {
        CAR -> tr("экран отдыхает, всё – после остановки", "the screen rests, everything after the stop")
        TAXI -> tr("такси, чужая машина", "a taxi, someone else's car")
        BUS -> tr("автобус, маршрутка, троллейбус", "bus, minibus, trolleybus")
        TRAM -> tr("трамвай, электричка, поезд", "tram, commuter train")
        METRO -> tr("от входа до выхода", "from the entrance to the exit")
        BIKE -> tr("экран отдыхает в пути", "the screen rests on the way")
    }

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: CAR }
}
