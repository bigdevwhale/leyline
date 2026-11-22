package app.leyline.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.leyline.MainActivity
import app.leyline.container
import app.leyline.domain.Ride
import app.leyline.domain.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Three pages: the hidden net under the city, how the road brews, and your first beast and home. */
@Composable
fun Onboarding() {
    var page by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf<String?>(null) }
    var ride by remember { mutableStateOf(Ride.CAR) }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Pic(when (page) { 0 -> "prologue_city"; 1 -> "prologue_brew"; else -> "prologue_ride" }, Modifier.fillMaxSize(), ContentScale.Crop)
            if (page == 2) Row(Modifier.align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Ride.CAR, Ride.BUS, Ride.TRAM, Ride.METRO, Ride.BIKE).forEach { r ->
                    val on = r == ride
                    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) C.paper else C.paper.copy(alpha = 0.7f))
                        .border(if (on) 2.dp else 1.dp, if (on) C.acc else C.glow.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { ride = r }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Pic(r.beast.pic(0), Modifier.size(52.dp))
                        Text(when (r) { Ride.CAR -> tr("Машина", "Car"); Ride.BUS -> tr("Автобус", "Bus"); Ride.TRAM -> tr("Трамвай", "Tram"); Ride.METRO -> tr("Метро", "Metro"); else -> tr("Велик", "Bike") },
                            style = T.tiny.copy(color = C.ink), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(14.dp).framed().padding(20.dp).navigationBarsPadding()) {
            when (page) {
                0 -> {
                    Text(tr("Под городом спит сеть силы", "A net of power sleeps under the city"), style = T.title)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Каждая ваша поездка – на работу, в магазин, к друзьям – зажигает по ней светящуюся тропу. Чем чаще ездите, тем ярче жилы.",
                        "Every ride you take – to work, to the shops, to friends – lights a glowing path along it. The more you ride, the brighter the lines."), style = T.body)
                }
                1 -> {
                    Text(tr("Дорога варит зелья", "The road brews potions"), style = T.title)
                    Spacer(Modifier.height(8.dp))
                    Line("ing_mist", tr("Проехали мост – речной туман, вдоль парка – живица, после заката – лунная пыль, в пробке – камень терпения.", "Over a bridge – river mist, past a park – resin, after sunset – moon dust, in a jam – a patience stone."))
                    Line("cauldron_brew", tr("Из ингредиентов в котле варятся эликсиры, обереги и подарки духам кварталов.", "In the cauldron they become elixirs, wards and gifts for the spirits of the quarters."))
                    Line("ic_hourglass", tr("За рулём ничего нажимать не нужно: всё ждёт остановки.", "Nothing to tap while driving: everything waits for the stop."))
                }
                else -> {
                    Text(tr("На чём вы ездите чаще всего?", "What do you ride most?"), style = T.title)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Из этого яйца вылупится ваш первый зверь: ${ride.beast.ru.lowercase()}. Остальные появятся, когда поедете по-другому. Местоположение нужно, чтобы найти кварталы вокруг; оно не уходит с телефона.",
                        "Your first beast hatches from this egg: a ${ride.beast.en.lowercase()}. The others come when you ride another way. Location finds the quarters around you; it never leaves the phone."), style = T.body)
                    if (busy) stage?.let { Spacer(Modifier.height(8.dp)); Text(it, style = T.smallB.copy(color = C.acc)) }
                    err?.let { Spacer(Modifier.height(8.dp)); Text(it, style = T.small.copy(color = C.bad)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == page) C.acc else C.line)) }
                }
                if (busy) CircularProgressIndicator(Modifier.size(28.dp), color = C.acc, strokeWidth = 3.dp)
                else Btn(if (page < 2) tr("Дальше", "Next") else tr("Начать", "Begin")) {
                    if (page < 2) { page++; return@Btn }
                    val want = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    MainActivity.ask(want) {
                        if (!c.feed.permitted()) { err = tr("Без местоположения не найти ни кварталы, ни дорогу.", "Without location neither the quarters nor the road can be found."); return@ask }
                        busy = true; err = null; stage = tr("Ищем вас…", "Finding you…")
                        scope.launch {
                            c.feed.acquire()
                            var f = c.game.live.value.fix ?: c.feed.last()
                            var waited = 0
                            while (f == null && waited < 40) { delay(500); waited++; f = c.game.live.value.fix }
                            c.feed.release()
                            if (f == null) { busy = false; err = tr("Не удалось определить место. Подойдите ближе к окну и попробуйте ещё раз.", "Couldn't get a position. Move closer to a window and try again."); return@launch }
                            stage = tr("Ищем ближайшие кварталы…", "Finding the nearest quarters…")
                            val ok = c.game.settle(f.lat, f.lon, ride)
                            busy = false
                            c.game.edit { it.copy(onboarded = true) }
                            // a busy map server doesn't stop the start: the quarters come later by themselves
                            if (!ok) c.game.news.tryEmit(app.leyline.data.News(tr("Сервер карт сейчас занят – кварталы подгрузятся сами чуть позже.", "The map server is busy – the quarters will load by themselves a little later.")))
                        }
                    }
                }
            }
        }
    }
}
