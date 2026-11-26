package app.leyline.domain

import app.leyline.data.Errand
import app.leyline.data.State
import app.leyline.trip.Geo

/**
 * The city's residents: twelve people who can't see the lines but know someone rides them. One settles in each of
 * the first quarters you keep coming back to (three visits). Each has a little story in three requests – a letter to
 * carry, ingredients to bring, a far ride, a potion from the cauldron – with no deadline: long errands, unlike the
 * spirits' steps. A finished story goes into the book of thanks.
 */
object Folk {
    const val VISITS = 3
    const val MAX_TAKEN = 3
    val power = listOf(50, 90, 160)
    /** Of the ingredient the resident's quarter is rich in. */
    val gifts = listOf(1, 2, 3)

    sealed class Ask {
        /** Carry a letter to another quarter (named before the request is taken). */
        object Letter : Ask()
        /** Bring [n] of ingredient [ing] here. */
        class Give(val ing: String, val n: Int) : Ask()
        /** Ride through a quarter at least [km] away, then come back here. */
        class Far(val km: Int = 5) : Ask()
        /** Bring a potion brewed in the cauldron. */
        class Potion(val recipe: String) : Ask()
    }

    /** One request: its words ({q} – the letter's quarter) and what it asks. */
    class Step(val ru: String, val en: String, val ask: Ask)

    class Resident(val ru: String, val en: String, val roleRu: String, val roleEn: String, val helloRu: String, val helloEn: String,
                   val thanksRu: String, val thanksEn: String, val steps: List<Step>) {
        val name get() = tr(ru, en)
        val role get() = tr(roleRu, roleEn)
        val hello get() = tr(helloRu, helloEn)
        val thanks get() = tr(thanksRu, thanksEn)
    }

    private fun letter(ru: String, en: String) = Step(ru, en, Ask.Letter)
    private fun give(ing: String, n: Int, ru: String, en: String) = Step(ru, en, Ask.Give(ing, n))
    private fun far(ru: String, en: String) = Step(ru, en, Ask.Far())
    private fun potion(recipe: String, ru: String, en: String) = Step(ru, en, Ask.Potion(recipe))

    val all = listOf(
        Resident("Аглая", "Aglaya", "вышивальщица", "the embroiderer",
            "Глаза у меня уже не те, а заказов полон дом. Поможешь, чародей?", "My eyes aren't what they were, and the house is full of orders. Will you help, wizard?",
            "Я вышила твоего зверя на платке – пусть бережёт тебя в дороге.", "I've embroidered your beast on a kerchief – may it keep you on the road.",
            listOf(
                letter("Отвези письмо сестре в квартал {q}. Почтальон теряет уже десятое.", "Take a letter to my sister in {q}. The postman has lost ten already."),
                give("dawn", 2, "Нитки путаются. Говорят, если смочить их росой зари, лягут ровно. Привези две капли.", "My threads keep tangling. They say dawn dew smooths them. Bring two drops."),
                give("moon", 3, "Хочу, чтобы узор светился в темноте. Для этого нужна лунная пыль – три щепотки.", "I want the pattern to glow in the dark. It takes moon dust – three pinches."),
            )),
        Resident("Степан", "Stepan", "отец трёх дочерей", "a father of three daughters",
            "Дочки всё заказали, а я из дома ни ногой – спина. Выручишь?", "My daughters have ordered it all, and I can't leave the house – my back. Will you help?",
            "Все три довольны, а младшая говорит, что ты – добрый волшебник. Так и есть.", "All three are happy, and the youngest says you're a kind wizard. So you are.",
            listOf(
                give("copper", 3, "Старшая просит медных бусин на платье. Три горсти.", "The eldest wants copper beads for a dress. Three handfuls."),
                give("resin", 2, "Средняя – живицы для духов: говорит, пахнет лесом. Два комочка.", "The middle one wants resin for perfume: she says it smells of the woods. Two lumps."),
                potion("seeker", "А младшая просит аленький цветочек. Такого нигде нет – но, говорят, капли искателя показывают то, чего нет на свете. Она согласна на них.",
                    "And the youngest asks for a little scarlet flower. There's none anywhere – but they say seeker's drops show what isn't in the world. She'll take those."),
            )),
        Resident("Кузьма", "Kuzma", "сторож моста", "the bridge keeper",
            "Сорок лет стерегу мост, а дальше него не бывал. Расскажешь, что там?", "Forty years I've kept this bridge and never been past it. Will you tell me what's out there?",
            "Теперь я знаю, что город больше моего моста. Спасибо, что был моими глазами.", "Now I know the city is bigger than my bridge. Thank you for being my eyes.",
            listOf(
                letter("Отнеси весточку старому другу в {q}. Мы не виделись с молодости.", "Take word to an old friend in {q}. We haven't met since we were young."),
                far("Съезди куда-нибудь подальше – не ближе 5 км отсюда – и расскажи, как там. Хочу знать, чем пахнут другие кварталы.", "Ride somewhere far – at least 5 km from here – and tell me what it's like. I want to know how other quarters smell."),
                letter("Друг ответил! Отвези ему моё письмо – он теперь живёт у внука, в квартале {q}.", "My friend wrote back! Take him my letter – he lives with his grandson now, in {q}."),
            )),
        Resident("Лиза", "Liza", "девочка с воздушным змеем", "a girl with a kite",
            "Твой зверь быстрее моего змея! Мне очень нужна помощь.", "Your beast is faster than my kite! I really need help.",
            "Змей летает выше всех во дворе. Я назвала его в честь твоего зверя!", "My kite flies higher than any in the yard. I named it after your beast!",
            listOf(
                letter("Отвези бабушке в {q} мой рисунок. Там нарисован твой зверь!", "Take my drawing to granny in {q}. Your beast is in it!"),
                give("wind", 3, "Змей не хочет взлетать. Привези ветра с быстрой дороги – три порыва.", "My kite won't take off. Bring wind from a fast road – three gusts."),
                potion("tailwind", "А чтобы летал всегда – попутный ветер в бутылке. Его варят в котле, я знаю!", "And so it always flies – a tailwind in a bottle. It's brewed in a cauldron, I know it is!"),
            )),
        Resident("Фёдор", "Fyodor", "лекарь", "the healer",
            "Больных много, а снадобий мало. Без тебя мне не справиться.", "Many patients, few remedies. I can't manage without you.",
            "Ты вылечил полквартала, даже не заходя в дома. Дверь лекарни для тебя открыта всегда.", "You've healed half the quarter without entering a house. My door is always open to you.",
            listOf(
                give("mist", 3, "Привези речного тумана – им сбивают жар. Три склянки, зима близко.", "Bring river mist – it brings fevers down. Three vials, winter is near."),
                far("Нужна трава, что растёт далеко. Съезди не ближе 5 км отсюда – и возвращайся, расскажешь, что видел.", "I need a herb that grows far away. Ride at least 5 km from here – then come back and tell me what you saw."),
                potion("harvest", "Последнее: зелье щедрой дороги. Полью им грядку с травами – вырастут вдвое.", "The last: a generous road potion. I'll water my herb bed with it – they'll grow twice as thick."),
            )),
        Resident("Марфа", "Marfa", "хозяйка чайной", "the teahouse keeper",
            "Открываю второй зал, а самовар один, и тот старый. Выручай!", "I'm opening a second room, and there's one samovar, and an old one at that. Help me out!",
            "Твоя чашка стоит на полке – наливаю в неё только тебе.", "Your cup stands on the shelf – I pour into it only for you.",
            listOf(
                give("stone", 2, "Принеси камней терпения: на них чай настаивается медленно и не горчит. Два хватит.", "Bring patience stones: tea steeps on them slowly and never turns bitter. Two will do."),
                give("copper", 2, "На новый самовар нужна медь – две горсти.", "The new samovar needs copper – two handfuls."),
                letter("Отвези приглашение на открытие в квартал {q} – там живут мои лучшие гости.", "Take an invitation to the opening to {q} – my best guests live there."),
            )),
        Resident("Тимофей", "Timofey", "ученик часовщика", "a clockmaker's apprentice",
            "Мастер уехал, а часы на башне встали. Если не починю – меня выгонят!", "My master's away and the tower clock has stopped. If I don't fix it, I'm out!",
            "Часы идут, мастер вернулся и ничего не заметил. Тссс!", "The clock runs, the master's back and noticed nothing. Shhh!",
            listOf(
                give("spark", 2, "Нужны искры кузни – завести пружину. Две.", "I need forge sparks to wind the spring. Two."),
                give("ore", 2, "И глубинная руда на новые шестерни – две крупицы.", "And deep ore for new cogs – two grains."),
                potion("longroad", "Чтобы часы шли долго и не уставали, капну в механизм отвара долгой дороги.", "So the clock runs long and never tires, I'll drip some long road brew into the works."),
            )),
        Resident("Василиса", "Vasilisa", "сказительница", "the storyteller",
            "Я знаю тысячу сказок, но ни одной – про тебя. Поможешь сочинить?", "I know a thousand tales but none about you. Will you help me make one?",
            "Теперь у города есть сказка о чародее на звере. Её рассказывают детям на ночь.", "Now the city has a tale of the wizard on a beast. It's told to children at bedtime.",
            listOf(
                letter("Отвези в квартал {q} начало моей новой сказки – пусть читают и ждут продолжения.", "Take the beginning of my new tale to {q} – let them read and wait for more."),
                give("ink", 3, "Чтобы записать сказку, нужны чернила мудрецов. Три склянки.", "To write the tale down I need sage's ink. Three vials."),
                give("shard", 1, "Конец сказки должен светиться. Привези звёздный осколок.", "The end of the tale must shine. Bring a star shard."),
            )),
        Resident("Пахом", "Pakhom", "рыбак без лодки", "a fisherman without a boat",
            "Лодку унесло в половодье. Строю новую – подсобишь?", "The flood took my boat. I'm building a new one – lend a hand?",
            "Лодка на воде! Первая рыба – твоя, и катаю тебя бесплатно до старости.", "The boat's on the water! The first fish is yours, and free rides until we're old.",
            listOf(
                give("stone", 3, "На якорь нужны камни потяжелее – камни терпения. Три.", "The anchor needs heavy stones – patience stones. Three."),
                give("resin", 2, "Швы смолят живицей. Два комочка.", "The seams are sealed with resin. Two lumps."),
                letter("Отвези в квартал {q} весть: лодка готова, пусть приходят кататься.", "Take word to {q}: the boat is ready, come for a ride."),
            )),
        Resident("Ника", "Nika", "садовница на крыше", "the rooftop gardener",
            "У меня сад на крыше, и он мал! Нужны семена издалека.", "I have a garden on the roof, and it's too small! I need seeds from afar.",
            "На крыше цветёт всё, что ты привёз. Приходи пить чай среди цветов.", "Everything you brought blooms on the roof. Come for tea among the flowers.",
            listOf(
                give("mist", 2, "Поливать нечем – колодец пересох. Привези речного тумана, две склянки.", "Nothing to water with – the well's dry. Bring river mist, two vials."),
                far("Семена нужны из дальних кварталов: съезди не ближе 5 км отсюда и возвращайся.", "The seeds must come from far quarters: ride at least 5 km from here and come back."),
                give("dawn", 2, "Чтобы цвело и зимой, нужна роса зари. Две капли.", "To bloom in winter too, it needs dawn dew. Two drops."),
            )),
        Resident("Гордей", "Gordey", "картограф-самоучка", "a self-taught mapmaker",
            "Рисую карту города, но дальше трёх улиц не хожу – ноги. Съездишь за меня?", "I'm drawing a map of the city but can't walk past three streets – my legs. Will you ride for me?",
            "Карта готова, и твой путь на ней – светящейся нитью.", "The map is done, and your way is on it as a glowing thread.",
            listOf(
                letter("Отвези мой вопрос в квартал {q}: какие там улицы? Пусть ответят на обороте.", "Take my question to {q}: what are its streets like? Let them answer on the back."),
                letter("Теперь – в квартал {q}. Там, говорят, есть улица, которой нет на картах.", "Now to {q}. They say there's a street there that's on no map."),
                give("feather", 2, "Последний штрих: два пера странника – начертить дальние дороги.", "The last touch: two wanderer's feathers, to draw the far roads."),
            )),
        Resident("Нюра", "Nyura", "бабушка у окна", "the grandmother at the window",
            "Внук уехал в другой квартал и не пишет. Сердце болит.", "My grandson moved to another quarter and doesn't write. My heart aches.",
            "Внук приезжает по воскресеньям. Это ты его привёз – я знаю.", "My grandson visits on Sundays. You brought him back – I know it.",
            listOf(
                letter("Отвези внуку письмо, он в квартале {q}.", "Take my grandson a letter, he's in {q}."),
                give("wax", 2, "Поставлю свечу на окно, чтобы он видел дорогу домой. Привези воску.", "I'll put a candle in the window so he can see the way home. Bring wax."),
                letter("Носки связаны. Отвези их в {q} – он переехал, вот новый адрес.", "The socks are knitted. Take them to {q} – he's moved, here's the new address."),
            )),
    )

    fun of(e: Errand) = all[e.who]
    fun step(e: Errand) = of(e).steps.getOrNull(e.step)
    fun done(e: Errand) = e.step >= of(e).steps.size
    fun at(s: State, quarter: String) = s.folk.firstOrNull { it.quarter == quarter }
    fun taken(s: State) = s.folk.count { it.since > 0 && !done(it) }

    /** The request's words with its quarter's name in. */
    fun text(s: State, e: Errand): String {
        val st = step(e) ?: return ""
        // the letter's quarter is known before the request is taken, so its words always name it; until then
        // [Errand.target] still holds the last letter's quarter
        val q = s.quarters[if (e.since > 0) e.target else pickTarget(s, e)]?.name.orEmpty()
        return tr(st.ru, st.en).replace("{q}", "«$q»")
    }

    /** A quarter visited often enough gets the next resident without a home, while any are left. */
    fun settle(s: State): State {
        val free = all.indices.filter { i -> s.folk.none { it.who == i } }
        if (free.isEmpty()) return s
        val ready = s.quarters.values.filter { it.known && it.visits >= VISITS && s.folk.none { e -> e.quarter == it.id } }
            .sortedBy { it.firstDay * 1000 + hash(it.id).mod(1000) }
        if (ready.isEmpty()) return s
        val add = ready.zip(free.shuffled(kotlin.random.Random(hash("folk:" + s.homeLat + ":" + s.homeLon)))).map { (q, i) -> Errand(q.id, i) }
        return s.copy(folk = s.folk + add)
    }

    /** Where a letter goes: another quarter 2–8 km away, any one if none is; never the last one again. */
    fun pickTarget(s: State, e: Errand): String {
        val home = s.quarters[e.quarter] ?: return ""
        val others = s.quarters.values.filter { it.id != home.id && it.id != e.target }.sortedBy { it.id }
        if (others.isEmpty()) return ""
        val near = others.filter { Geo.distance(home.lat, home.lon, it.lat, it.lon) in 2000.0..8000.0 }.ifEmpty { others }
        return near[Rand(hash("letter:${e.quarter}:${e.step}")).int(near.size)].id
    }

    /** A ride entered quarter [id]: a letter for it is delivered, a far request is met far enough away. */
    fun entered(s: State, id: String): List<Errand> {
        val q = s.quarters[id] ?: return s.folk
        return s.folk.map { e ->
            if (e.since == 0L || e.arrived || done(e)) return@map e
            val hit = when (val a = step(e)?.ask) {
                Ask.Letter -> e.target == id
                is Ask.Far -> s.quarters[e.quarter]?.let { h -> Geo.distance(h.lat, h.lon, q.lat, q.lon) >= a.km * 1000 } == true
                else -> false
            }
            if (hit) e.copy(arrived = true) else e
        }
    }

    /** (done so far, needed) for a taken request. */
    fun progress(s: State, e: Errand): Pair<Int, Int> = when (val a = step(e)?.ask) {
        Ask.Letter, is Ask.Far -> (if (e.arrived) 1 else 0) to 1
        is Ask.Give -> minOf(s.has(a.ing), a.n) to a.n
        is Ask.Potion -> minOf(s.stock[a.recipe] ?: 0, 1) to 1
        null -> 1 to 1
    }

    /** Ready to report: a letter delivered (from anywhere); the rest only in the resident's own quarter. */
    fun ready(s: State, e: Errand): Boolean {
        if (e.since == 0L || done(e)) return false
        val (a, b) = progress(s, e)
        return a >= b && (step(e)?.ask == Ask.Letter || s.here == e.quarter)
    }

    /** The ingredient the resident's quarter is rich in: part of the reward. */
    fun gift(s: State, e: Errand): String? = s.quarters[e.quarter]?.kind?.let { Book.of(it) }

    /** Plain words on how to do it. */
    fun how(s: State, e: Errand): String {
        val home = s.quarters[e.quarter]
        return when (val a = step(e)?.ask) {
            Ask.Letter -> s.quarters[e.target]?.let { q ->
                val km = home?.let { Geo.distance(it.lat, it.lon, q.lat, q.lon) / 1000 } ?: 0.0
                tr("Нажмите «В путь» и проедьте через квартал «${q.ru}» (${fmt1(km)} км отсюда). Письмо отдадут само, а отчитаться можно откуда угодно.",
                    "Tap «Set off» and ride through «${q.en}» (${fmt1(km)} km from here). The letter delivers itself; report back from anywhere.")
            }.orEmpty()
            is Ask.Give -> Book.ing(a.ing).let { g ->
                tr("Соберите: ${g.ru.lowercase()} – ${a.n} шт. ${g.howRu}. Отдать можно здесь, в квартале «${home?.ru}».",
                    "Gather ${g.en.lowercase()} ×${a.n}. ${g.howEn}. Hand it over here, in «${home?.en}».")
            }
            is Ask.Far -> if (e.arrived) tr("Вы съездили далеко. Теперь вернитесь сюда, в «${home?.ru}», и расскажите.", "You've been far. Now come back here, to «${home?.en}», and tell.")
                else tr("Проедьте через любой квартал не ближе ${a.km} км отсюда, потом вернитесь сюда, в «${home?.ru}».",
                    "Ride through any quarter at least ${a.km} km from here, then come back here, to «${home?.en}».")
            is Ask.Potion -> Book.recipe(a.recipe).let { r ->
                if (r.id in s.known) tr("Сварите «${r.ru}» в гримуаре и отдайте здесь, в квартале «${home?.ru}».", "Brew «${r.en}» in the grimoire and hand it over here, in «${home?.en}».")
                else tr("Рецепта «${r.ru}» ещё нет в гримуаре – его находят опытами в котле. Сварите и отдайте здесь, в квартале «${home?.ru}».",
                    "«${r.en}» isn't in the grimoire yet – recipes are found by experiment in the cauldron. Brew it and hand it over here, in «${home?.en}».")
            }
            null -> ""
        }
    }

    /** The picture of the current request. */
    fun pic(e: Errand): String = when (val a = step(e)?.ask) {
        Ask.Letter -> "ic_scroll"
        is Ask.Give -> Book.ing(a.ing).pic
        is Ask.Far -> "ic_compass"
        is Ask.Potion -> Book.recipe(a.recipe).pic
        null -> "ic_star"
    }
}

/**
 * Full pages of the grimoire: every recipe of a kind found gives power once and a small lasting favour; all of
 * them, the keeper's title. Granted by the game itself, so pages filled before this came in count too.
 */
object Pages {
    val CATS = listOf(Cat.ELIXIR, Cat.AWAKEN, Cat.WARD, Cat.GIFT)
    const val SET_POWER = 250
    const val ALL_POWER = 1000

    fun complete(s: State, c: Cat) = Book.recipes.filter { it.cat == c }.all { it.id in s.known }
    fun allFound(s: State) = Book.recipes.all { it.id in s.known }
    fun flag(c: Cat) = "set:" + c.name.lowercase()

    /** What a full page of [c] gives for good. */
    fun perk(c: Cat): String = when (c) {
        Cat.ELIXIR -> tr("эликсиры действуют на одну поездку дольше", "elixirs work for one ride longer")
        Cat.AWAKEN -> tr("звери растут на 10% быстрее", "beasts grow 10% faster")
        Cat.WARD -> tr("каждый оберег даёт на 1 ингредиент больше", "every ward gives one ingredient more")
        Cat.GIFT -> tr("духи-друзья дают по 2 ингредиента вместо одного", "friendly spirits give 2 ingredients instead of one")
        Cat.WONDER -> ""
    }

    val keeper get() = tr("Хранитель гримуара", "Keeper of the grimoire")

    /** Gives what full pages are owed; [say] hears about each. */
    fun grant(s0: State, say: (String) -> Unit = {}): State {
        var s = s0
        for (c in CATS) if (flag(c) !in s.told && complete(s, c)) {
            s = s.copy(power = s.power + SET_POWER, told = s.told + flag(c))
            say(tr("Страница «${c.pluralRu}» заполнена: +$SET_POWER силы, и отныне ${perk(c)}", "The «${c.pluralEn}» page is full: +$SET_POWER power, and from now on ${perk(c)}"))
        }
        if ("set:all" !in s.told && allFound(s)) {
            s = s.copy(power = s.power + ALL_POWER, told = s.told + "set:all")
            say(tr("Все рецепты найдены! Вы – ${keeper.lowercase()}: +$ALL_POWER силы", "Every recipe found! You are the ${keeper.lowercase()}: +$ALL_POWER power"))
        }
        return s
    }
}
