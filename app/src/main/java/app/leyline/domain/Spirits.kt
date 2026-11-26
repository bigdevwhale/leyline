package app.leyline.domain

/**
 * The guardian spirits of the quarters, one for each kind. A spirit notices you the first time you ride
 * through one of its quarters and then asks four things, one after another. The fourth is always its gift
 * from the cauldron; after it the spirit is your friend and adds its ingredient whenever you pass its quarters.
 */
class Spirit(val kind: Kind, val ru: String, val en: String, val greetRu: String, val greetEn: String, val steps: List<Step>) {
    val id get() = kind.id
    val name get() = tr(ru, en)
    val greet get() = tr(greetRu, greetEn)
    val pic get() = "spirit_$id"
    val ing get() = Book.of(kind)
    val gift get() = "g_$id"
}

/** What a step asks. [n] is how many; [what] an ingredient or a condition id. */
enum class Ask { PASS, GIVE, RIDE, GIFT }

class Step(val ask: Ask, val n: Int, val what: String, val sayRu: String, val sayEn: String) {
    val say get() = tr(sayRu, sayEn)
}

object Spirits {
    private fun pass(n: Int, ru: String, en: String) = Step(Ask.PASS, n, "", ru, en)
    private fun give(ing: String, n: Int, ru: String, en: String) = Step(Ask.GIVE, n, ing, ru, en)
    private fun ride(cond: String, ru: String, en: String) = Step(Ask.RIDE, 1, cond, ru, en)
    private fun gift(ru: String, en: String) = Step(Ask.GIFT, 1, "", ru, en)

    val all = listOf(
        Spirit(Kind.GARDEN, "Мшистый олень", "Moss Stag",
            "Тише, тише… Ты проехал по моим аллеям – и листья обернулись тебе вслед.", "Hush… You rode along my alleys, and the leaves turned to watch you.", listOf(
                pass(2, "Проезжай мимо моих садов почаще – мне нравится, как гудит твой зверь.", "Ride past my gardens more often – I like the hum of your beast."),
                give("resin", 3, "Принеси мне живицы: я залечу ею старый дуб.", "Bring me resin: I'll heal the old oak with it."),
                ride("dawn", "Покажись мне на заре – утром мои травы поют.", "Show yourself to me at dawn – my grasses sing in the morning."),
                gift("Сплети мне венок из живицы, росы и солнца.", "Weave me a wreath of resin, dew and sun."))),
        Spirit(Kind.WATER, "Выдра", "Otter",
            "Плюх! Ты пересёк мою реку, а я и не заметила, как ты подкрался.", "Splash! You crossed my river before I even noticed you.", listOf(
                pass(2, "Приезжай к воде ещё – я научу тебя слушать течение.", "Come to the water again – I'll teach you to listen to the current."),
                give("mist", 3, "Набери мне речного тумана: в нём прячутся мои рыбы.", "Gather me some river mist: my fish hide in it."),
                ride("bridge", "Проедь по мосту – я люблю смотреть снизу на тех, кто едет над водой.", "Ride over a bridge – I love watching from below those who cross the water."),
                gift("Свари мне туманную жемчужину.", "Brew me a mist pearl."))),
        Spirit(Kind.OLD, "Сова-свечница", "Candle Owl",
            "Уху. Эти стены помнят многое. Теперь они помнят и тебя.", "Hoo. These walls remember much. Now they remember you too.", listOf(
                pass(2, "Проезжай старыми улицами – им приятно, когда о них не забывают.", "Ride the old streets – they like not being forgotten."),
                give("dust", 3, "Принеси мне пыли веков – я храню в ней истории.", "Bring me dust of ages – I keep stories in it."),
                ride("night", "Проедь через старый город ночью: при луне я вижу лучше.", "Ride through the old town at night: I see better by moonlight."),
                gift("Свари мне свечу веков – мою старую я сожгла до огарка.", "Brew me a candle of ages – mine has burnt down to a stub."))),
        Spirit(Kind.SCHOLAR, "Кот-звездочёт", "Stargazer Cat",
            "Мрр. Я как раз записывал звёзды – и вдруг ты. Запишу и тебя.", "Mrr. I was just writing down the stars – and here you are. I'll write you down too.", listOf(
                pass(2, "Заглядывай в мои кварталы – здесь думается лучше.", "Drop by my quarters – thinking is easier here."),
                give("ink", 3, "Принеси чернил мудрецов: мои закончились на самом интересном месте.", "Bring me sage's ink: mine ran out at the best part."),
                ride("ink", "Проедь мимо библиотеки или университета – я хочу знать, что там нового.", "Ride past a library or a university – I want to know what's new there."),
                gift("Свари мне звёздные чернила.", "Brew me star ink."))),
        Spirit(Kind.FORGE, "Саламандра", "Salamander",
            "Ш-ш-ш! Осторожно, горячо. Хотя твой зверь, вижу, огня не боится.", "Hiss! Careful, it's hot. Though your beast, I see, isn't afraid of fire.", listOf(
                pass(2, "Проезжай мимо кузниц и заводов – мне скучно без гостей.", "Ride past the forges and factories – I'm bored without guests."),
                give("spark", 3, "Принеси мне искр, а то огонь в горне совсем притих.", "Bring me sparks, the fire in the forge has gone quiet."),
                ride("spark", "Проедь вдоль заводов или депо – погреешься.", "Ride along the factories or a depot – you'll warm up."),
                gift("Выкуй мне огонёк в котле.", "Forge me a flame in the cauldron."))),
        Spirit(Kind.BAZAAR, "Сорока-меняла", "Trader Magpie",
            "Стрекочу-стрекочу! Новенький! А что у тебя в карманах блестит?", "Chatter-chatter! A new one! What's shining in your pockets?", listOf(
                pass(2, "Приезжай на торжища – тут всегда есть на что поглядеть.", "Come to the markets – there's always something to look at."),
                give("copper", 4, "Принеси меди. Блестящей. Побольше.", "Bring copper. Shiny. Lots of it."),
                ride("weekend", "Проедь через торжище в выходной – тогда тут веселее всего.", "Ride through a market at the weekend – that's when it's the most fun."),
                gift("Свари мне медный амулет. Самый блестящий.", "Brew me a copper amulet. The shiniest."))),
        Spirit(Kind.GATE, "Журавль-странник", "Wanderer Crane",
            "Курлы. Я встречаю всех, кто приезжает и уезжает. Здравствуй, путник.", "Kroo. I greet everyone who comes and goes. Hello, traveller.", listOf(
                pass(2, "Проезжай мимо вокзалов – там начинаются все дороги.", "Ride past the stations – that's where all roads begin."),
                give("feather", 3, "Принеси мне перьев странника – подлатаю крылья перед перелётом.", "Bring me wanderer's feathers – I'll mend my wings before the flight."),
                ride("long", "Соверши долгую поездку – от 10 км.", "Make a long ride – 10 km or more."),
                gift("Свари мне компас странника, чтобы я не сбился с пути.", "Brew me a wanderer's compass, so I never lose my way."))),
        Spirit(Kind.HOMES, "Ёж-домовик", "Hearth Hedgehog",
            "Пых-пых. Тут живут люди, и я за ними приглядываю. И за тобой пригляжу.", "Puff-puff. People live here, and I look after them. I'll look after you too.", listOf(
                pass(2, "Проезжай мимо моих дворов – мне спокойнее, когда свои рядом.", "Ride past my yards – I feel calmer with friends nearby."),
                give("wax", 2, "Принеси воску: зимой у меня свечи на вес золота.", "Bring wax: in winter my candles are worth their weight in gold."),
                ride("home", "Вернись домой – в святилище, где ты бываешь чаще всего.", "Come home – to the sanctum you visit most."),
                gift("Свари оберег очага.", "Brew a hearth charm."))),
    )
    val byId = all.associateBy { it.id }
    fun of(k: Kind) = byId.getValue(k.id)

    /** The plain how-to of a step, never in riddles. */
    fun how(sp: Spirit, st: Step): String = when (st.ask) {
        Ask.PASS -> tr("Проедьте через квартал вида «${sp.kind.ru}» в ${st.n} разных поездках. Подойдёт любой такой квартал, а не только этот.",
            "Ride through a «${sp.kind.en}» quarter on ${st.n} different rides. Any quarter of that kind counts, not just this one.")
        Ask.GIVE -> tr("Соберите ${Book.ing(st.what).ru.lowercase()} – ${st.n} шт. – и нажмите «Отдать». ${Book.ing(st.what).howRu}.",
            "Gather ${Book.ing(st.what).en.lowercase()} ×${st.n} and tap «Give». ${Book.ing(st.what).howEn}.")
        Ask.RIDE -> when (st.what) {
            "dawn" -> tr("Проедьте через квартал «${sp.kind.ru}» на рассвете или на закате (±45 минут от восхода или захода солнца).", "Ride through a «${sp.kind.en}» quarter at sunrise or sunset (within 45 minutes).")
            "bridge" -> tr("Проедьте через квартал «${sp.kind.ru}» так, чтобы поездка принесла речной туман: через мост или вдоль реки.", "Ride through a «${sp.kind.en}» quarter so that the ride brings river mist: over a bridge or along a river.")
            "night" -> tr("Проедьте через квартал «${sp.kind.ru}» после заката.", "Ride through a «${sp.kind.en}» quarter after sunset.")
            "ink" -> tr("Проедьте через квартал «${sp.kind.ru}» так, чтобы поездка принесла чернила мудрецов: мимо вуза, библиотеки или школы.", "Ride through a «${sp.kind.en}» quarter so that the ride brings sage's ink: past a university, library or school.")
            "spark" -> tr("Проедьте через квартал «${sp.kind.ru}» так, чтобы поездка принесла искру кузни: мимо заводов или депо.", "Ride through a «${sp.kind.en}» quarter so that the ride brings a forge spark: past factories or a depot.")
            "weekend" -> tr("Проедьте через квартал «${sp.kind.ru}» в субботу или воскресенье.", "Ride through a «${sp.kind.en}» quarter on Saturday or Sunday.")
            "long" -> tr("Одна поездка длиной от 10 км, в любой части города.", "One ride of 10 km or more, anywhere in the city.")
            else -> tr("Закончите поездку у святилища, где вы бываете чаще всего.", "End a ride at the sanctum you visit most.")
        }
        Ask.GIFT -> tr("Сварите в гримуаре «${Book.recipe(sp.gift).ru}» и нажмите «Отдать».", "Brew «${Book.recipe(sp.gift).en}» in the grimoire and tap «Give».")
    }

    /** What finishing step [i] brings: power and some of the spirit's ingredient. */
    fun reward(i: Int): Pair<Int, Int> = when (i) { 0 -> 50 to 2; 1 -> 100 to 2; 2 -> 150 to 3; else -> 300 to 5 }

    /** What a friendly spirit gives on a ride through its quarters: one, two with the full page of gifts. */
    fun friendGift(s: app.leyline.data.State) = if (Pages.complete(s, Cat.GIFT)) 2 else 1

    /**
     * What a friend gives for its gift brewed once more: a little power and a handful of its ingredient. The gift
     * takes about three of that ingredient and one other, so it is a small trade, never a way to breed ingredients.
     */
    val OFFER = 40 to 4

    fun offerLine(sp: Spirit) = tr("Подношение: +${OFFER.first} силы и ${Book.ing(sp.ing).ru.lowercase()} ×${OFFER.second}",
        "Offering: +${OFFER.first} power and ${Book.ing(sp.ing).en.lowercase()} ×${OFFER.second}")

    fun friendLine(sp: Spirit, n: Int = 1) = tr("Друг: +$n ${Book.ing(sp.ing).ru.lowercase()} в каждой поездке через его кварталы",
        "Friend: +$n ${Book.ing(sp.ing).en.lowercase()} on every ride through its quarters")
}
