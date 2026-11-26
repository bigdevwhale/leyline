package app.leyline.domain

/**
 * Experiments. Nobody hands you the grimoire: you throw two or three different ingredients into the cauldron
 * (one of each) and see what comes out. A right set writes its recipe down; a wrong one makes a dud, a little
 * power and a hint about how close you were. The great elixir writes itself when every other page is filled.
 */
object Lab {
    fun key(ids: Collection<String>) = ids.sorted().joinToString("+")

    /** The recipe made of exactly these ingredients, if any (the great elixir can't be found this way). */
    fun match(ids: Collection<String>): Recipe? = ids.toSet().let { set -> Book.recipes.firstOrNull { it.id != "great" && it.needs.keys == set } }

    /**
     * How close a failed set is to some page not written yet: 4 – one ingredient missing, 3 – one too many,
     * 2 – two belong together, 1 – cold, 0 – not even close. Counted against what is known now, so an old dud's
     * hint stays true as the grimoire fills.
     */
    fun warmth(ids: Collection<String>, known: Set<String>): Int {
        val set = ids.toSet()
        val rest = Book.recipes.filter { it.id != "great" && it.id !in known }
        return when {
            rest.any { it.needs.keys.containsAll(set) } -> 4
            rest.any { set.containsAll(it.needs.keys) } -> 3
            rest.any { (it.needs.keys intersect set).size >= 2 } -> 2
            rest.any { (it.needs.keys intersect set).isNotEmpty() } -> 1
            else -> 0
        }
    }

    fun hint(ids: Collection<String>, known: Set<String>): String = when (warmth(ids, known)) {
        4 -> tr("Тепло! Всё на своём месте, но чего-то не хватает – нужен ещё один ингредиент.", "Warm! Everything belongs, but something is missing – one more ingredient is needed.")
        3 -> tr("Тепло! Один ингредиент тут лишний.", "Warm! One ingredient here is too many.")
        2 -> tr("Два ингредиента явно дружат, третий им мешает.", "Two of them clearly get along, the third is in the way.")
        1 -> tr("Холодно. Вместе они ни во что не складываются.", "Cold. Together they add up to nothing.")
        else -> tr("Совсем мимо.", "Not even close.")
    }

    /** The ingredients of a tried set ("a+b+c"). */
    fun parts(key: String) = key.split('+').filter { it.isNotEmpty() }

    private val DUDS = listOf(
        "Мутная жижа|Murky sludge", "Кисель с искрами|Sparkly jelly", "Пар с запахом носков|Sock-scented steam", "Очень серьёзный пузырь|A very serious bubble",
        "Суп из тумана|Fog soup", "Квакающая пена|Croaking foam", "Липкая радуга|Sticky rainbow", "Чихающий дым|Sneezing smoke",
        "Горелая каша мага|Wizard's burnt porridge", "Бульк|A blorp",
    )
    fun dud(key: String) = bi(DUDS[Rand(hash(key)).int(DUDS.size)])
    const val DUD_POWER = 5
}

/**
 * Clues to recipes not found yet: one ingredient of one page («one of the elixirs takes wind»). A spirit gives one
 * for each of its first three requests, a beast when it awakens, a resident at the end of their story. A page
 * never gets every ingredient this way – the last one is still found in the cauldron. Gifts are left out: the
 * spirits tell those themselves. Kept in [app.leyline.data.State.clues] as "recipe:ingredient".
 */
object Clues {
    fun of(clues: Set<String>, r: Recipe): List<String> = r.needs.keys.filter { "${r.id}:$it" in clues }

    /** Pages that can still get a clue. */
    fun open(s: app.leyline.data.State) = Book.recipes.filter {
        it.id != "great" && it.cat != Cat.GIFT && it.id !in s.known && of(s.clues, it).size < it.needs.size - 1
    }

    /** The next clue, the same for the same moment of the game; null when there is nothing left to tell. */
    fun next(s: app.leyline.data.State): String? {
        val pool = open(s)
        if (pool.isEmpty()) return null
        val r = Rand(hash("clue:${s.clues.size}:${s.known.size}"))
        val page = pool[r.int(pool.size)]
        val left = page.needs.keys.filter { "${page.id}:$it" !in s.clues }.sorted()
        return "${page.id}:${left[r.int(left.size)]}"
    }

    /** Gives the next clue, if any, and says it. */
    fun give(s: app.leyline.data.State, say: (String) -> Unit): app.leyline.data.State {
        val c = next(s) ?: return s
        say(tr("Подсказка: ", "A clue: ") + line(c))
        return s.copy(clues = s.clues + c)
    }

    /** «в один из эликсиров идёт ветер». */
    fun line(c: String): String {
        val r = Book.recipe[c.substringBefore(':')] ?: return ""
        val ing = Book.ing[c.substringAfter(':')] ?: return ""
        val ru = when (r.cat) { Cat.ELIXIR -> "в один из эликсиров"; Cat.AWAKEN -> "в одно из пробуждений"; Cat.WARD -> "в один из оберегов"; Cat.WONDER -> "в одно из чудес"; Cat.GIFT -> "в один из даров" }
        return tr("$ru идёт ${ing.ru.lowercase()}", "one of the ${r.cat.pluralEn.lowercase()} takes ${ing.en.lowercase()}")
    }
}

/** Cozy things for the tower: where the pile of ingredients goes. [x],[y] place the bottom middle in the room picture (0..1), [w] is the width. */
class Decor(val id: String, val ru: String, val en: String, val needs: Map<String, Int>, val power: Int, val x: Float, val y: Float, val w: Float) {
    val name get() = tr(ru, en)
    val pic get() = "decor_$id"
}

object Decors {
    val all = listOf(
        Decor("garland", "Гирлянда звёзд", "Star garland", mapOf("moon" to 3, "wax" to 2), 60, 0.5f, 0.262f, 0.4f),
        Decor("lamp", "Бумажный фонарь", "Paper lantern", mapOf("wax" to 3, "sun" to 1), 40, 0.205f, 0.63f, 0.09f),
        Decor("books", "Стопка книг со свечой", "Books with a candle", mapOf("ink" to 3, "dust" to 1), 50, 0.8f, 0.645f, 0.1f),
        Decor("basket", "Лежанка зверя", "Beast's basket", mapOf("resin" to 3, "copper" to 2), 60, 0.4f, 0.63f, 0.17f),
        Decor("mushrooms", "Горшок светящихся грибов", "Glowing mushrooms", mapOf("mist" to 3, "resin" to 2), 70, 0.69f, 0.64f, 0.1f),
        Decor("teatable", "Чайный столик", "Tea table", mapOf("copper" to 4, "storm" to 2), 90, 0.56f, 0.625f, 0.14f),
        Decor("mobile", "Подвеска с лунами", "Moon mobile", mapOf("moon" to 4, "feather" to 2), 100, 0.705f, 0.33f, 0.11f),
        Decor("cage", "Клетка с огоньком", "Wisp cage", mapOf("spark" to 3, "wind" to 3), 110, 0.295f, 0.345f, 0.10f),
        Decor("ball", "Хрустальный шар", "Crystal ball", mapOf("shard" to 2, "ink" to 2, "moon" to 2), 140, 0.5f, 0.425f, 0.075f),
        Decor("chest", "Сундук с пледом", "Blanket chest", mapOf("stone" to 5, "dust" to 3), 150, 0.31f, 0.59f, 0.13f),
        Decor("telescope", "Телескоп", "Telescope", mapOf("shard" to 3, "spark" to 3, "dawn" to 2), 200, 0.67f, 0.59f, 0.13f),
        Decor("clock", "Лунные часы", "Moon clock", mapOf("frost" to 2, "dust" to 4, "lily" to 2), 250, 0.8f, 0.575f, 0.085f),
    )
    val byId = all.associateBy { it.id }

    /** Every cozy thing adds 2% to the power of a ride. */
    fun bonus(n: Int) = 1.0 + 0.02 * n
}

/** Surprises of the road, decided by the ride's own moment, so a ride always tells the same story. */
object Luck {
    /** A quarter that blooms this week with an ingredient: among those already ridden, the same for the whole week. */
    fun bloom(visited: List<String>, day: Long = today()): Pair<String, String>? {
        if (visited.size < 2) return null
        val week = weekStart(day)
        val r = Rand(hash("bloom:$week"))
        val q = visited.sorted()[r.int(visited.size)]
        val pool = listOf("shard", "dawn", "moon", "lily", "ink", "spark", "feather", "wax", "dust", "wind")
        return q to pool[r.int(pool.size)]
    }
}
