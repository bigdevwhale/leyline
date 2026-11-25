package app.leyline.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leyline.BuildConfig
import app.leyline.container
import app.leyline.data.State
import app.leyline.domain.Book
import app.leyline.domain.Decors
import app.leyline.domain.Mounts
import app.leyline.domain.Omen
import app.leyline.domain.Recipe
import app.leyline.domain.Ride
import app.leyline.domain.Sky
import app.leyline.domain.Spirits
import app.leyline.domain.dur
import app.leyline.domain.fmt
import app.leyline.domain.fmt1
import app.leyline.domain.tr
import app.leyline.trip.TripService
import kotlinx.coroutines.launch

/** The beast's picture for now: asleep after a quiet week, the stage otherwise. */
fun beastPic(s: State, beast: String): String {
    val m = s.mounts[beast] ?: return app.leyline.domain.Beast.of(beast).pic(0)
    return if (Mounts.asleep(m)) m.kind.sleepPic else m.kind.pic(m.stage)
}

/** A gentle breathing bob for the creatures. */
@Composable
fun bob(period: Int = 2600, amount: Float = 4f): Float {
    if (BuildConfig.SHOTS) return 0f
    val t = rememberInfiniteTransition(label = "bob")
    val v by t.animateFloat(0f, amount, infiniteRepeatable(tween(period), RepeatMode.Reverse), label = "bob")
    return v
}

/** The tower room: the scene fills the screen, small glass cards float over it – what to brew next and who is waiting. */
@Composable
fun Tower(s: State, open: (Over) -> Unit) {
    val now = System.currentTimeMillis()
    val night = Sky.dark(now, s.homeLat, s.homeLon)
    val omen = Omen.of()
    val g = LocalGame
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Room(if (night) "den_night" else "den", s.decor, Modifier.fillMaxSize())
        Fireflies(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x881A0E1E), 0.22f to Color.Transparent, 0.62f to Color.Transparent, 1f to Color(0xCC1A0E1E))))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("pot_power", fmt(s.power)) { open(Over.Journal) }
                Pill(Book.ing(omen).pic, tr("Знак дня", "Omen")) { open(Over.Omen) }
                Spacer(Modifier.weight(1f))
                HelpBtn()
                Box(Modifier.size(36.dp).clip(CircleShape).background(C.paper.copy(alpha = 0.78f)).border(1.dp, C.glow.copy(alpha = 0.5f), CircleShape).clickable { open(Over.Settings) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Settings, tr("Настройки", "Settings"), tint = C.ink, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            // a spirit's whisper, floating under the window
            Box(Modifier.fillMaxWidth().framed(20.dp).padding(horizontal = 12.dp, vertical = 8.dp)) { SpiritLine(s, open) }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                // the sigil, a round window of its own
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xCC2A1F33)).border(1.dp, C.glow.copy(alpha = 0.7f), CircleShape).clickable { open(Over.Sigil) }, contentAlignment = Alignment.Center) {
                    if (s.mirror) SigilMark(s, Modifier.size(40.dp)) else Pic("ic_sigil", Modifier.size(34.dp), alpha = 0.7f)
                }
                Spacer(Modifier.width(8.dp))
                // comforts: things for the room
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xCC2A1F33)).border(1.dp, C.glow.copy(alpha = 0.7f), CircleShape).clickable { open(Over.Cozy) }, contentAlignment = Alignment.Center) {
                    Pic("decor_lamp", Modifier.size(36.dp))
                    if (Decors.all.any { g.canBuy(s, it) }) Box(Modifier.align(Alignment.TopEnd).size(13.dp).clip(CircleShape).background(C.glow))
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.buffs.forEach { (id, n) -> Pill(Book.recipe(id).pic, "×$n") { open(Over.Recipe(id)) } }
                }
            }
            Spacer(Modifier.weight(1f))
            // the rug: the beast and the cauldron
            val y = bob()
            Row(Modifier.fillMaxWidth().height(210.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1.3f).fillMaxSize().clickable { Nav.tab = 3 }, contentAlignment = Alignment.BottomCenter) {
                    Pic(beastPic(s, s.beast), Modifier.fillMaxSize().offset(y = (-y).dp))
                }
                val brewable = Book.recipes.any { it.id in s.known && g.canBrew(s, it.id) && !wonderDone(s, it.id) }
                Box(Modifier.weight(1f).height(150.dp).clickable { Nav.tab = 2 }, contentAlignment = Alignment.BottomCenter) {
                    Pic(if (brewable) "cauldron_brew" else "cauldron_idle", Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().framed(20.dp).padding(horizontal = 12.dp, vertical = 10.dp)) { NextUp(s, open) }
            Spacer(Modifier.navigationBarsPadding().height(Dock.H + 8.dp))
        }
    }
}

/** Game rules reachable from composables without passing the container around. */
val LocalGame: app.leyline.data.Game @Composable get() = LocalContext.current.container.game

/** What to brew next: something ready now, or the nearest recipe and what it lacks. */
@Composable
private fun NextUp(s: State, open: (Over) -> Unit) {
    val g = LocalGame
    val scope = rememberCoroutineScope()
    val ready = Book.recipes.firstOrNull { it.id in s.known && g.canBrew(s, it.id) && !wonderDone(s, it.id) }
    if (ready != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(ready.pic, Modifier.size(42.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable { open(Over.Recipe(ready.id)) }) {
                Text(tr("Можно сварить", "Ready to brew"), style = T.tiny)
                Text(ready.name, style = T.h3)
            }
            Btn(tr("Сварить", "Brew"), small = true) { scope.launch { g.brew(ready.id) } }
        }
        return
    }
    val near = nearest(s)
    if (near == null) {
        val kinds = s.bag.count { it.value > 0 }
        val allFound = Book.recipes.all { it.id in s.known }
        Row(Modifier.clickable { Nav.tab = 2 }, verticalAlignment = Alignment.CenterVertically) {
            Pic("pot_unknown", Modifier.size(46.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(when { allFound -> tr("Гримуар заполнен", "The grimoire is full"); kinds >= 2 -> tr("Пора в котёл", "Time for the cauldron"); else -> tr("Котёл ждёт", "The cauldron waits") }, style = T.h3)
                Text(when {
                    allFound -> tr("Все рецепты найдены. Вы – великий чародей.", "Every recipe found. You are a great wizard.")
                    kinds >= 2 -> tr("Бросьте в котёл два-три разных ингредиента: вдруг получится новое зелье.", "Drop two or three different ingredients into the cauldron: a new potion may come out.")
                    else -> tr("Съездите куда-нибудь: дорога принесёт ингредиенты для первых опытов.", "Take a ride: the road will bring ingredients for your first experiments.")
                }, style = T.tiny, maxLines = 3)
            }
            if (kinds >= 2 && !allFound) Text("›", style = T.title.copy(color = C.acc))
        }
        return
    }
    val (r, missing) = near
    val first = missing.firstOrNull { it.first != "power" }
    Row(Modifier.clickable { open(Over.Recipe(r.id)) }, verticalAlignment = Alignment.CenterVertically) {
        Pic(if (r.id in s.known) r.pic else "pot_unknown", Modifier.size(46.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (r.id in s.known) r.name else tr("Скоро откроется новый рецепт", "A new recipe is close"), style = T.h3)
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                missing.take(4).forEach { (k, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pic(if (k == "power") "pot_power" else Book.ing(k).pic, Modifier.size(24.dp), alpha = if (k == "power" || k in s.seen) 1f else 0.55f)
                        Text(" ×$n", style = T.smallB)
                    }
                }
            }
            if (first != null) Text(tr("${Book.ing(first.first).ru}: ", "${Book.ing(first.first).en}: ") + Book.ing(first.first).how.replaceFirstChar { it.lowercase() }, style = T.tiny, maxLines = 2)
            else Text(tr("Не хватает силы – она копится от километров", "Not enough power yet – it builds up with kilometres"), style = T.tiny)
        }
    }
}

fun wonderDone(s: State, id: String) = when (id) { "aurora" -> s.aurora; "mirror" -> s.mirror; "great" -> s.great; else -> false }

/** The recipe closest to being brewed and what it still lacks ("power" for power). */
fun nearest(s: State): Pair<Recipe, List<Pair<String, Int>>>? {
    fun lacks(r: Recipe) = r.needs.mapNotNull { (k, n) -> (n - s.has(k)).takeIf { it > 0 }?.let { k to it } } +
        listOfNotNull((r.power - s.power).takeIf { it > 0 }?.let { "power" to it })
    // only pages already written: the rest are found in the cauldron
    val pool = Book.recipes.filter { it.id in s.known && !wonderDone(s, it.id) && (s.brewed[it.id] ?: 0) == 0 }
    return pool.map { it to lacks(it) }.filter { it.second.isNotEmpty() }
        .minByOrNull { (r, l) -> l.sumOf { if (it.first == "power") it.second / 50 else it.second } + (if (r.id in s.known) 0 else 2) }
}

@Composable
private fun SpiritLine(s: State, open: (Over) -> Unit) {
    val g = LocalGame
    val waiting = s.spirits.keys.firstOrNull { g.spiritReady(s, it) }
    when {
        waiting != null -> {
            val sp = Spirits.byId.getValue(waiting)
            Row(Modifier.fillMaxWidth().clickable { open(Over.Spirit(waiting)) }, verticalAlignment = Alignment.CenterVertically) {
                Pic(sp.pic, Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("${sp.ru} ждёт вас", "${sp.en} is waiting"), style = T.smallB)
                    Text(tr("Просьба выполнена – загляните за наградой", "The request is done – come for the reward"), style = T.tiny)
                }
                Text("›", style = T.title.copy(color = C.acc))
            }
        }
        s.spirits.isNotEmpty() -> Row(Modifier.fillMaxWidth().clickable { open(Over.Spirits) }, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) { s.spirits.keys.take(4).forEach { Pic(Spirits.byId.getValue(it).pic, Modifier.size(34.dp)) } }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Духи кварталов: ${s.spirits.size} из 8", "Spirits of the quarters: ${s.spirits.size} of 8"), style = T.smallB)
                Text(tr("Друзей: ${s.friends().size}. Тапните – их просьбы", "Friends: ${s.friends().size}. Tap for their requests"), style = T.tiny)
            }
            Text("›", style = T.title.copy(color = C.acc))
        }
        else -> Row(Modifier.fillMaxWidth().clickable { open(Over.Spirits) }, verticalAlignment = Alignment.CenterVertically) {
            Pic("ic_lantern", Modifier.size(34.dp))
            Spacer(Modifier.width(10.dp))
            Text(tr("Проезжайте через разные кварталы – вас заметят их духи.", "Ride through different quarters – their spirits will notice you."), style = T.small.copy(color = C.ink), modifier = Modifier.weight(1f))
        }
    }
}

/** A ride is on: a calm screen with the beast, the kilometres and what the road has given so far. */
@Composable
fun OnTheWay(s: State, locked: Boolean, open: (Over) -> Unit = {}) {
    val c = LocalContext.current.container
    val t = s.trip ?: return
    val live by c.game.live.collectAsState()
    val ride = Ride.of(t.ride)
    val q = s.quarters[s.here]
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1220))) {
        Pic("prologue_ride", Modifier.fillMaxSize(), ContentScale.Crop, alpha = 0.38f)
        Fireflies(Modifier.fillMaxSize(), 26)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x991A1220), 0.5f to Color.Transparent, 1f to Color(0xEE1A1220))))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(q?.name ?: tr("Дорога", "The road"), style = T.title.copy(color = Color(0xFFF7EDDC)), textAlign = TextAlign.Center)
            Text(ride.title, style = T.small.copy(color = Color(0xFFD9C7D4)))
            Spacer(Modifier.weight(1f))
            val y = bob(1400, 6f)
            Pic(beastPic(s, ride.beast.id).let { if (live.moving && it.endsWith("grown")) ride.beast.joyPic else it }, Modifier.size(230.dp).offset(y = (-y).dp))
            Spacer(Modifier.height(10.dp))
            Text(fmt1(t.km), style = T.huge.copy(color = Color(0xFFF7EDDC), fontSize = 64.sp))
            Text(tr("км · ${dur(((System.currentTimeMillis() - t.start) / 1000).toInt())} в пути", "km · ${dur(((System.currentTimeMillis() - t.start) / 1000).toInt())} on the way"), style = T.body.copy(color = Color(0xFFD9C7D4)))
            Spacer(Modifier.weight(1f))
            if (!locked) {
                val got = c.game.soFar(t)
                Text(if (got.isEmpty()) tr("Дорога собирает силу. Карта вдоль пути прочитается после остановки.", "The road gathers power. The map along the way is read after the stop.")
                else tr("Уже собрано:", "Gathered so far:"), style = T.small.copy(color = Color(0xFFD9C7D4)), textAlign = TextAlign.Center)
                if (got.isNotEmpty()) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    got.forEach { (k, n) -> Row(verticalAlignment = Alignment.CenterVertically) { Pic(Book.ing(k).pic, Modifier.size(34.dp)); Text("×$n", style = T.smallB.copy(color = Color(0xFFF7EDDC))) } }
                }
                Spacer(Modifier.height(16.dp))
                if (ride == Ride.METRO) {
                    // under the ground: no GPS, the stations at both ends
                    val st = c.game.metroStart()
                    Text(when {
                        st != null -> tr("Вход: «${st.ru}»", "In at: ${st.en}")
                        t.lastAt > 0 -> tr("Вход отмечен", "The entrance is marked")
                        else -> tr("Вход ещё не найден", "The entrance isn't found yet")
                    }, style = T.smallB.copy(color = Color(0xFFF7EDDC)), textAlign = TextAlign.Center)
                    Ghost(if (t.lastAt > 0) tr("Другая станция", "Another station") else tr("Выбрать станцию", "Pick the station"), color = Color(0xFFFFD27A)) { open(Over.Station(exit = false)) }
                    Spacer(Modifier.height(6.dp))
                    Btn(tr("Приехали", "Arrived"), Modifier.fillMaxWidth()) { open(Over.Station(exit = true)) }
                    Text(tr("GPS спит до выхода, батарейка отдыхает. На выходе нажмите «Приехали» – из приложения или из уведомления.",
                        "The GPS sleeps till the exit and the battery rests. Out there tap «Arrived» – here or in the notification."),
                        style = T.tiny.copy(color = Color(0xFFB8A3B4)), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Btn(tr("Приехали", "Arrived"), Modifier.fillMaxWidth()) { TripService.stop(c.app) }
                    Text(tr("Можно убрать телефон: поездка идёт и с выключенным экраном. Постоите 8 минут – закончится сама.",
                        "You can pocket the phone: the ride goes on with the screen off. Stand still for 8 minutes and it ends by itself."),
                        style = T.tiny.copy(color = Color(0xFFB8A3B4)), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.navigationBarsPadding().height(Dock.H))
            } else {
                Text(tr("Смотрите на дорогу. Всё самое интересное – после остановки.", "Eyes on the road. Everything interesting waits for the stop."),
                    style = T.body.copy(color = Color(0xFFD9C7D4)), textAlign = TextAlign.Center)
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

/** Choosing how you ride; each way has its own beast. */
@Composable
fun RidesSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    Sheet(onDismiss) {
        Title(tr("Как едете?", "How are you riding?"), tr("У каждого способа свой зверь. Он растёт от ваших поездок.", "Each way has its own beast. It grows with your rides."))
        Spacer(Modifier.height(8.dp))
        Ride.entries.forEach { r ->
            val m = s.mounts[r.beast.id]
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (r.id == s.ride) C.paper2 else Color.Transparent)
                .clickable {
                    app.leyline.Manifests.askLocation {
                        if (c.feed.permitted()) { TripService.start(c.app, r); onDismiss() }
                    }
                }.padding(vertical = 6.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(if (m != null) beastPic(s, r.beast.id) else r.beast.pic(0), Modifier.size(54.dp), alpha = if (m != null) 1f else 0.45f)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = T.bodyB)
                    Text(if (m == null) tr("${r.sub} · вылупится ${r.beast.ru.lowercase()}", "${r.sub} · a ${r.beast.en.lowercase()} will hatch") else r.sub, style = T.small)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("Поездка закончится сама, если постоять 8 минут, или по кнопке «Приехали». В метро GPS не нужен: хватит станций входа и выхода. Машину по Bluetooth можно выбрать в настройках.",
            "The ride ends by itself after 8 still minutes, or with «Arrived». The metro needs no GPS: the stations in and out are enough. The car's Bluetooth can be chosen in settings."), style = T.tiny)
    }
}
