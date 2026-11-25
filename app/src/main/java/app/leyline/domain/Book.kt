package app.leyline.domain

/**
 * An ingredient the road gives. [how] says in plain words where it comes from; [osm] ones are read from
 * the map along the route, the rest from the ride itself (speed, time, weather).
 */
class Ingredient(val id: String, val ru: String, val en: String, val howRu: String, val howEn: String, val osm: Boolean = false) {
    val name get() = tr(ru, en)
    val how get() = tr(howRu, howEn)
    val pic get() = "ing_$id"
}

enum class Cat(val ru: String, val en: String, val pluralRu: String, val pluralEn: String) {
    ELIXIR("Эликсир", "Elixir", "Эликсиры", "Elixirs"),
    AWAKEN("Пробуждение", "Awakening", "Пробуждения", "Awakenings"),
    WARD("Оберег", "Ward", "Обереги", "Wards"),
    GIFT("Дар духу", "Gift", "Дары духам", "Gifts"),
    WONDER("Чудо", "Wonder", "Чудеса", "Wonders");

    val title get() = tr(ru, en)
    val plural get() = tr(pluralRu, pluralEn)
}

/** A recipe of the grimoire: what goes into the cauldron, how much power it takes and what it does. */
class Recipe(
    val id: String, val cat: Cat, val needs: Map<String, Int>, val power: Int,
    val ru: String, val en: String, val doesRu: String, val doesEn: String, val pic: String,
) {
    val name get() = tr(ru, en)
    val does get() = tr(doesRu, doesEn)
}

object Book {
    val ingredients = listOf(
        Ingredient("wind", "Ветер", "Wind", "Быстрая дорога: от 50 км/ч, по 1 за каждые 2 км", "A fast road: 50 km/h and up, 1 for every 2 km"),
        Ingredient("stone", "Камень терпения", "Patience stone", "Пробки и долгие остановки: по 1 за каждые 4 минуты", "Traffic jams and long stops: 1 for every 4 minutes"),
        Ingredient("mist", "Речной туман", "River mist", "Мост, набережная, речка рядом с дорогой. И туманный день", "A bridge, an embankment, a river by the road. And a foggy day", osm = true),
        Ingredient("resin", "Живица", "Resin", "Парк, сквер или лес вдоль дороги", "A park, a square or woods along the road", osm = true),
        Ingredient("moon", "Лунная пыль", "Moon dust", "Поездка после заката. В полнолуние вдвое больше", "A ride after sunset. Twice as much at full moon"),
        Ingredient("dawn", "Роса зари", "Dawn dew", "Поездка на рассвете или на закате", "A ride at sunrise or sunset"),
        Ingredient("storm", "Грозовая вода", "Storm water", "Поездка в дождь. Или в сильный ветер, от 30 км/ч", "A ride in the rain. Or in a strong wind, 30 km/h and up"),
        Ingredient("frost", "Иней", "Hoarfrost", "Поездка в снег или в мороз. Или холодной ночью, от +10° и ниже", "A ride in snow or frost. Or on a cold night, +10° and below"),
        Ingredient("sun", "Солнечный янтарь", "Sun amber", "Днём в ясную погоду", "In the daytime under a clear sky"),
        Ingredient("ore", "Глубинная руда", "Deep ore", "Метро и тоннели. Или квартал кузниц после заката", "The underground and tunnels. Or a forge quarter after dark"),
        Ingredient("wax", "Свечной воск", "Candle wax", "Храм, часовня или собор по пути", "A church, a chapel or a cathedral on the way", osm = true),
        Ingredient("dust", "Пыль веков", "Dust of ages", "Памятники, старые здания, музеи вдоль дороги", "Monuments, old buildings, museums along the road", osm = true),
        Ingredient("ink", "Чернила мудрецов", "Sage's ink", "Университет, библиотека или школа по пути", "A university, a library or a school on the way", osm = true),
        Ingredient("spark", "Искра кузни", "Forge spark", "Заводы, мастерские, депо вдоль дороги", "Factories, workshops, depots along the road", osm = true),
        Ingredient("feather", "Перо странника", "Wanderer's feather", "Вокзал, станция или автовокзал по пути", "A railway station, a halt or a bus station on the way", osm = true),
        Ingredient("lily", "Бледная лилия", "Pale lily", "Кладбище рядом с дорогой", "A cemetery by the road", osm = true),
        Ingredient("copper", "Медь торжища", "Market copper", "Улицы с магазинами и рынки", "Streets with shops and markets", osm = true),
        Ingredient("shard", "Звёздный осколок", "Star shard", "Новые места: улицы, где вы ещё не ездили", "New places: streets you've never ridden"),
    )
    val ing = ingredients.associateBy { it.id }

    /** What only the sky gives: somewhere it never snows, so the cauldron can distil them from the rest. */
    val WEATHER = listOf("storm", "frost", "sun")
    /** This many of one ingredient distil into one of the sky's. */
    const val DISTIL = 4

    /** What [target] is distilled from: the ingredient there is most of, if there's enough of it. */
    fun distilFrom(bag: Map<String, Int>, target: String): String? =
        if (target !in WEATHER) null
        else bag.filterKeys { it != target && it in ing }.filterValues { it >= DISTIL }.maxByOrNull { it.value }?.key
    fun ing(id: String) = ing.getValue(id)

    val recipes = listOf(
        Recipe("tailwind", Cat.ELIXIR, mapOf("wind" to 3, "feather" to 1), 0, "Попутный ветер", "Tailwind",
            "Три следующие поездки дают вдвое больше силы", "The next three rides give twice the power", "pot_tailwind"),
        Recipe("harvest", Cat.ELIXIR, mapOf("resin" to 2, "sun" to 1, "copper" to 2), 0, "Зелье щедрой дороги", "Generous road",
            "Следующая поездка соберёт вдвое больше ингредиентов", "The next ride gathers twice the ingredients", "pot_harvest"),
        Recipe("seeker", Cat.ELIXIR, mapOf("ink" to 2, "dust" to 2), 0, "Капли искателя", "Seeker's drops",
            "Следующая поездка принесёт два звёздных осколка", "The next ride brings two star shards", "pot_seeker"),
        Recipe("longroad", Cat.ELIXIR, mapOf("stone" to 3, "mist" to 1), 0, "Отвар долгой дороги", "Long road brew",
            "Три следующие поездки растят зверя в полтора раза быстрее", "The next three rides grow your beast half as fast again", "pot_longroad"),

        Recipe("awaken1", Cat.AWAKEN, mapOf("spark" to 2, "dawn" to 1), 100, "Искра пробуждения", "Spark of awakening",
            "Детёныш становится юным зверем", "A baby grows into a young beast", "pot_awaken1"),
        Recipe("awaken2", Cat.AWAKEN, mapOf("moon" to 2, "wax" to 2, "stone" to 2), 300, "Сердце зверя", "Beast's heart",
            "Юный зверь становится взрослым", "A young beast grows up", "pot_awaken2"),
        Recipe("awaken3", Cat.AWAKEN, mapOf("shard" to 3, "dawn" to 2, "wind" to 3), 800, "Древняя кровь", "Ancient blood",
            "Взрослый зверь становится древним", "A grown beast becomes ancient", "pot_awaken3"),

        Recipe("w_grove", Cat.WARD, mapOf("resin" to 3, "dawn" to 1), 150, "Священная роща", "Sacred grove",
            "Святилище даёт живицу в каждой поездке, что начинается или кончается у него", "The sanctum gives resin on every ride that starts or ends there", "ward_grove"),
        Recipe("w_well", Cat.WARD, mapOf("mist" to 3, "lily" to 1), 150, "Туманный колодец", "Misty well",
            "Святилище даёт речной туман в каждой поездке, что начинается или кончается у него", "The sanctum gives river mist on every ride that starts or ends there", "ward_well"),
        Recipe("w_lantern", Cat.WARD, mapOf("moon" to 3, "wax" to 1), 150, "Лунный фонарь", "Moon lantern",
            "Святилище даёт лунную пыль в каждой поездке, что начинается или кончается у него", "The sanctum gives moon dust on every ride that starts or ends there", "ward_lantern"),
        Recipe("w_menhir", Cat.WARD, mapOf("stone" to 4, "dust" to 1), 150, "Менгир", "Menhir",
            "Святилище даёт камень терпения в каждой поездке, что начинается или кончается у него", "The sanctum gives a patience stone on every ride that starts or ends there", "ward_menhir"),
        Recipe("w_bell", Cat.WARD, mapOf("wax" to 3, "copper" to 2), 150, "Звонница", "Bell tower",
            "Святилище даёт свечной воск в каждой поездке, что начинается или кончается у него", "The sanctum gives candle wax on every ride that starts or ends there", "ward_bell"),
        Recipe("w_crystal", Cat.WARD, mapOf("shard" to 2, "spark" to 1), 200, "Кристальный обелиск", "Crystal obelisk",
            "Святилище даёт звёздный осколок в каждой поездке, что начинается или кончается у него", "The sanctum gives a star shard on every ride that starts or ends there", "ward_crystal"),

        Recipe("g_garden", Cat.GIFT, mapOf("resin" to 2, "dawn" to 1, "sun" to 1), 0, "Венок живицы", "Resin wreath",
            "Подарок Мшистому оленю, духу садов", "A gift for the Moss Stag, spirit of gardens", "gift_garden"),
        Recipe("g_water", Cat.GIFT, mapOf("mist" to 3, "moon" to 1), 0, "Туманная жемчужина", "Mist pearl",
            "Подарок Выдре, духу набережных", "A gift for the Otter, spirit of the riverside", "gift_water"),
        Recipe("g_old", Cat.GIFT, mapOf("dust" to 2, "wax" to 2), 0, "Свеча веков", "Candle of ages",
            "Подарок Сове-свечнице, духу старого города", "A gift for the Candle Owl, spirit of the old town", "gift_old"),
        Recipe("g_scholar", Cat.GIFT, mapOf("ink" to 3, "moon" to 1), 0, "Звёздные чернила", "Star ink",
            "Подарок Коту-звездочёту, духу учёных кварталов", "A gift for the Stargazer Cat, spirit of scholars", "gift_scholar"),
        Recipe("g_forge", Cat.GIFT, mapOf("spark" to 3, "stone" to 1), 0, "Кованый огонёк", "Forged flame",
            "Подарок Саламандре, духу кузниц", "A gift for the Salamander, spirit of forges", "gift_forge"),
        Recipe("g_bazaar", Cat.GIFT, mapOf("copper" to 3, "feather" to 1), 0, "Медный амулет", "Copper amulet",
            "Подарок Сороке-меняле, духу торжищ", "A gift for the Trader Magpie, spirit of markets", "gift_bazaar"),
        Recipe("g_gate", Cat.GIFT, mapOf("feather" to 3, "sun" to 1), 0, "Компас странника", "Wanderer's compass",
            "Подарок Журавлю-страннику, духу ворот и вокзалов", "A gift for the Wanderer Crane, spirit of gates", "gift_gate"),
        Recipe("g_homes", Cat.GIFT, mapOf("wax" to 1, "resin" to 1, "stone" to 1), 0, "Оберег очага", "Hearth charm",
            "Подарок Ежу-домовику, духу слобод", "A gift for the Hearth Hedgehog, spirit of homesteads", "gift_homes"),

        Recipe("aurora", Cat.WONDER, mapOf("frost" to 2, "moon" to 2, "storm" to 1), 300, "Северное сияние", "Bottled aurora",
            "Жилы на карте загораются цветами сияния", "The lines on the map glow in the colours of the aurora", "pot_aurora"),
        Recipe("mirror", Cat.WONDER, mapOf("shard" to 2, "mist" to 2, "dust" to 1), 300, "Зеркало пути", "Mirror of the road",
            "Открывает вашу печать – фигуру из мест, где вы бываете чаще всего", "Reveals your sigil – the shape of the places you go most", "pot_mirror"),
        Recipe("great", Cat.WONDER, ingredients.associate { it.id to 1 }, 1000, "Великий эликсир", "The great elixir",
            "По одному всего, что даёт дорога. Сваривший его – великий чародей, а звери светятся золотом", "One of everything the road gives. Whoever brews it is a great wizard, and the beasts glow gold", "pot_great"),
    )
    val recipe = recipes.associateBy { it.id }
    fun recipe(id: String) = recipe.getValue(id)

    /** Kinds of quarters and the ingredient each is rich in. */
    fun of(k: Kind) = when (k) {
        Kind.GARDEN -> "resin"; Kind.WATER -> "mist"; Kind.OLD -> "dust"; Kind.SCHOLAR -> "ink"
        Kind.FORGE -> "spark"; Kind.BAZAAR -> "copper"; Kind.GATE -> "feather"; Kind.HOMES -> "wax"
    }
}
