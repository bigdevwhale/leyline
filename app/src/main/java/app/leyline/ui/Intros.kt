package app.leyline.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.leyline.domain.tr

/** A first-visit introduction: what a screen is for and how to use it. */
class Intro(val id: String, val pic: String, val title: String, val lines: List<Pair<String, String>>)

/** The introduction for what is on screen now, or null. */
fun introFor(tab: Int, over: Over?, trip: Boolean): Intro? = when (over) {
    is Over.TripPage -> Intro("i.trip", "ic_scroll", tr("Что сплела дорога", "What the road wove"), listOf(
        "ing_wind" to tr("После каждой поездки – карточка: путь, сила и ингредиенты.", "After every ride – a card: the way, power and ingredients."),
        "ing_mist" to tr("Мосты, парки, храмы и вокзалы приложение читает с карты вдоль пути – это занимает несколько секунд.", "Bridges, parks, churches and stations are read from the map along the way – it takes a few seconds."),
        "pot_power" to tr("Сила – от километров. Ею платят за пробуждение зверей, обереги и чудеса.", "Power comes from kilometres. It pays for awakening beasts, wards and wonders.")))
    is Over.Spirit -> Intro("i.spirit", "spirit_garden", tr("Дух квартала", "A quarter's spirit"), listOf(
        "ic_scroll" to tr("У каждого духа четыре просьбы. Под каждой – «Как выполнить» простыми словами.", "Each spirit has four requests. Under each – «How to do it» in plain words."),
        "ic_star" to tr("Выполнили – нажмите «Забрать награду» или «Отдать». Последняя просьба – подарок из котла.", "Done – tap «Take the reward» or «Give». The last request is a gift from the cauldron."),
        "ic_lantern" to tr("Друг-дух добавляет свой ингредиент в каждой поездке через его кварталы.", "A spirit friend adds its ingredient on every ride through its quarters.")))
    is Over.Sanctum -> Intro("i.sanctum", "ic_shrine", tr("Святилище", "A sanctum"), listOf(
        "ic_shrine" to tr("Место, где поездки часто начинаются и кончаются. Пробуждается после трёх.", "A place where rides often start and end. It awakens after three."),
        "ward_grove" to tr("Поставьте оберег – и каждая поездка отсюда или сюда даст его ингредиент. Тот же оберег ещё раз – уровень выше.", "Place a ward and every ride from or to here gives its ingredient. The same ward again raises its level.")))
    is Over.Folk, Over.FolkList -> Intro("i.folk", "ic_scroll", tr("Жители", "Residents"), listOf(
        "ic_scroll" to tr("В кварталах, где вы бываете часто, появляются жители. У каждого – история из трёх просьб, и ни одна не горит.", "Residents turn up in quarters you ride through often. Each has a story of three requests, and none is in a hurry."),
        "ic_compass" to tr("Письмо засчитывается, когда вы проедете через нужный квартал, а отчитаться можно откуда угодно. Остальное отдают на месте, в квартале жителя.", "A letter counts when you ride through its quarter, and you can report from anywhere. The rest is handed over in the resident's own quarter."),
        "ic_star" to tr("Сразу можно вести до трёх просьб. Законченные истории остаются в книге благодарностей.", "You can carry up to three requests at once. Finished stories stay in the book of thanks.")))
    is Over.Recipe, is Over.Ingredient, is Over.Quarter, is Over.Station, Over.Spirits, Over.Omen, Over.Sigil, Over.Journal, Over.Settings, Over.Cozy -> null
    Over.Rides -> Intro("i.rides", "ic_compass", tr("В путь", "Setting off"), listOf(
        "mount_car_baby" to tr("Выберите, как едете. Первая поездка каждым способом выведет нового зверя.", "Pick how you ride. The first ride each way hatches a new beast."),
        "ic_hourglass" to tr("За рулём экран отдыхает: всё важное – после остановки. Поездка закончится сама, если постоять 20 минут.", "Driving, the screen rests: everything waits for the stop. The ride ends by itself after 20 still minutes.")))
    null -> when (tab) {
        0 -> if (trip) Intro("i.ontrip", "ic_compass", tr("В пути", "On the way"), listOf(
            "ing_wind" to tr("Ветер, камни пробок и лунная пыль копятся прямо в пути.", "Wind, jam stones and moon dust gather right on the way."),
            "ic_hourglass" to tr("Можно убрать телефон: поездка идёт и с выключенным экраном.", "You can pocket the phone: the ride goes on with the screen off.")))
        else Intro("i.tower", "tab_home", tr("Башня", "The tower"), listOf(
            "ic_compass" to tr("Перед поездкой нажмите «В путь» и выберите, как едете: машина, автобус, трамвай, метро, велосипед.", "Before a ride tap «Set off» and pick how you ride: car, bus, tram, underground, bike."),
            "ing_mist" to tr("Дорога сама собирает ингредиенты: мост даёт туман, парк – живицу, ночь – лунную пыль.", "The road gathers ingredients by itself: a bridge gives mist, a park resin, the night moon dust."),
            "cauldron_brew" to tr("Тапните котёл – там ставят опыты и находят рецепты. Забурлил – значит, есть что сварить.", "Tap the cauldron – that's where you experiment and find recipes. If it bubbles, something can be brewed."),
            "decor_lamp" to tr("Фонарик слева – уют: лишние ингредиенты превращаются в вещи для комнаты.", "The lantern on the left is comfort: spare ingredients turn into things for the room."),
            "ic_omen" to tr("Знак дня: один ингредиент сегодня приходит вдвое чаще.", "The omen: one ingredient comes twice as often today.")))
        1 -> Intro("i.map", "tab_map", tr("Карта силы", "The map of power"), listOf(
            "tab_map" to tr("Каждая поездка зажигает жилу вдоль пути. Чем чаще ездите – тем она ярче.", "Every ride lights a line along the way. The more you ride it, the brighter it gets."),
            "ic_star" to tr("Где сходятся три яркие жилы – узел силы: проезд через него даёт больше силы.", "Where three bright lines meet – a knot: passing it gives more power."),
            "ic_shrine" to tr("Святилища – ваши частые места. Тапните, чтобы поставить оберег.", "Sanctums are your frequent places. Tap one to place a ward."),
            "spirit_water" to tr("Тапните квартал – кто его дух и чем богата там дорога.", "Tap a quarter – who its spirit is and what the road there gives.")))
        2 -> Intro("i.lab", "tab_book", tr("Гримуар и котёл", "The grimoire and the cauldron"), listOf(
            "cauldron_brew" to tr("Рецепты никто не подскажет: бросьте в котёл два-три разных ингредиента и нажмите «Варить».", "Nobody hands you recipes: drop two or three different ingredients into the cauldron and tap «Brew»."),
            "pot_unknown" to tr("Угадали состав – рецепт записан. Не угадали – выйдет брак и подсказка: тепло или холодно.", "Guess a set right and the recipe is written down. Wrong – you get a dud and a hint: warm or cold."),
            "pot_tailwind" to tr("«Рецепты» – варить найденное. «Запас» – сваренное: эликсиры пьют перед поездкой, обереги ставят на карте.", "«Recipes» – brew what you've found. «Stock» – what's brewed: elixirs before a ride, wards on the map.")))
        else -> Intro("i.stable", "tab_stable", tr("Звери", "The beasts"), listOf(
            "mount_car_young" to tr("У каждого способа езды – свой зверь. Он растёт от километров.", "Each way of riding has its beast. It grows with kilometres."),
            "pot_awaken1" to tr("Наездил нужное – сварите зелье пробуждения, и зверь перейдёт на новую ступень.", "Ridden enough – brew an awakening potion and the beast reaches a new stage."),
            "ing_moon" to tr("Черты зверь получает от того, как вы ездите: ночью, в дождь, быстро, в пробках.", "A beast earns traits from how you ride: at night, in the rain, fast, in jams.")))
    }
}

@Composable
fun IntroSheet(i: Intro, onDismiss: () -> Unit) {
    // a compact card near the top, lightly dimmed: the screen it explains stays in view below it
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val shown = androidx.compose.runtime.remember { androidx.compose.animation.core.MutableTransitionState(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown.targetState = true }
    Box(Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(shown, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x33000000)).tap(onDismiss))
        }
        androidx.compose.animation.AnimatedVisibility(shown, Modifier.align(Alignment.TopCenter),
            enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { -it }) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
                .framed(16.dp).tap { }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                IntroBody(i, onDismiss)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.IntroBody(i: Intro, onDismiss: () -> Unit) {
    run {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(i.pic, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text(i.title, style = T.h3, modifier = Modifier.weight(1f))
            Text(tr("Понятно", "Got it"), style = T.smallB.copy(color = C.acc), modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
        }
        Spacer(Modifier.height(4.dp))
        i.lines.forEach { (pic, text) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(pic, Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, style = T.small.copy(color = C.ink))
            }
        }
    }
}

/** «?» on a screen opens its introduction again. */
object Help { var open by mutableStateOf(false) }

@Composable
fun HelpBtn(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).clip(CircleShape).background(C.paper.copy(alpha = 0.78f)).border(1.dp, C.glow.copy(alpha = 0.5f), CircleShape)
        .clickable { Help.open = true }, contentAlignment = Alignment.Center) {
        Text("?", style = T.h3)
    }
}
