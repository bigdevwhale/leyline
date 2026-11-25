package app.leyline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.leyline.container
import app.leyline.data.Game
import app.leyline.data.State
import app.leyline.domain.Metro
import app.leyline.domain.Station
import app.leyline.domain.tr
import app.leyline.trip.Fix
import app.leyline.trip.Geo
import app.leyline.trip.TripService
import kotlinx.coroutines.launch

/**
 * By metro: where the ride began ([exit] false) or where it came out. The exit first tries one position; the
 * stations of the city are always there to pick from, nearest first. With no entrance known, the exit asks for
 * it first – an exit alone makes no ride.
 */
@Composable
fun StationSheet(s: State, exit: Boolean, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val g = c.game
    val t = s.trip ?: return
    val scope = rememberCoroutineScope()
    val from = !exit || t.lastAt == 0L
    var query by remember { mutableStateOf("") }
    var seeking by remember { mutableStateOf(true) }
    var here by remember { mutableStateOf<Fix?>(null) }
    // null while loading; asked again on «Try again», or once the phone finds itself when nothing else said where
    var load by remember { mutableStateOf<Game.StationsLoad?>(null) }
    var tries by remember { mutableIntStateOf(0) }
    LaunchedEffect(tries) { load = null; load = g.stations(here?.lat, here?.lon) }
    LaunchedEffect(from) {
        seeking = true
        here = c.feed.once(25_000)?.takeIf { it.accuracy <= Metro.GOOD }
        seeking = false
        if (here != null && load == Game.StationsLoad.NO_PLACE) tries++
    }
    val near = here?.let { Metro.nearest(s.stations, it.lat, it.lon) }

    fun choose(lat: Double, lon: Double) {
        if (from) { scope.launch { g.metroFrom(lat, lon) }; if (!exit) onDismiss() }
        else { TripService.arrive(c.app, lat, lon); onDismiss() }
    }

    Sheet(onDismiss) {
        Title(if (from) tr("Откуда едете?", "Where did you set off?") else tr("Где вышли?", "Where did you come out?"),
            if (exit && from) tr("Не знаю, где вы вошли, а без начала поездка не засчитается. Сначала станция входа.",
                "I don't know where you went in, and a ride needs a start. The entrance station first.")
            else tr("Под землёй GPS не нужен: хватит станции.", "No GPS under the ground: a station is enough."))
        Spacer(Modifier.height(6.dp))
        val f = here
        when {
            seeking -> Text(tr("Ищу, где вы…", "Finding where you are…"), style = T.small)
            f != null -> Btn(near?.let { tr("Здесь: «${it.ru}»", "Here: ${it.en}") } ?: tr("Здесь, где я сейчас", "Here, where I am now"), Modifier.fillMaxWidth()) {
                choose(near?.lat ?: f.lat, near?.lon ?: f.lon)
            }
            else -> Text(tr("Спутников не слышно – выберите станцию.", "No satellites to be heard – pick the station."), style = T.small)
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(query, { query = it.take(40) }, singleLine = true, textStyle = T.body, cursorBrush = SolidColor(C.acc),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.paper2).border(1.dp, C.line, RoundedCornerShape(10.dp)).padding(10.dp),
            decorationBox = { inner -> if (query.isEmpty()) Text(tr("Станция", "Station"), style = T.body.copy(color = C.mute)); inner() })
        Spacer(Modifier.height(6.dp))
        // nearest to where the phone is, or else to where the ride began (for the exit) or home
        val ref = here?.let { it.lat to it.lon } ?: t.takeIf { it.lastAt > 0 && !from }?.let { it.firstLat to it.firstLon }
            ?: (s.homeLat to s.homeLon).takeIf { it.first != 0.0 }
        val list = Metro.pick(s.stations, query, ref?.first, ref?.second).take(40)
        when {
            s.stations.isEmpty() -> when (load) {
                null -> Text(tr("Загружаю станции…", "Loading the stations…"), style = T.small)
                Game.StationsLoad.NO_PLACE -> Text(if (seeking) tr("Ищу, в каком вы городе…", "Finding which city you're in…")
                    else tr("Не знаю, в каком вы городе: спутников не слышно. Поднимитесь к выходу и попробуйте ещё раз.",
                        "I don't know which city you're in: no satellites to be heard. Go up to the exit and try again."), style = T.small)
                Game.StationsLoad.NO_NET -> Text(tr("Сервер карты не ответил – он бывает перегружен. Попробуйте ещё раз через минуту.",
                    "The map's server didn't answer – it's often busy. Try again in a minute."), style = T.small)
                Game.StationsLoad.OK -> Text(tr("Вокруг не нашлось станций метро.", "No metro stations found around."), style = T.small)
            }
            list.isEmpty() -> Text(tr("Такой станции нет.", "No such station."), style = T.small)
        }
        if (s.stations.isEmpty() && (load == Game.StationsLoad.NO_NET || (load == Game.StationsLoad.NO_PLACE && !seeking)))
            Ghost(tr("Попробовать ещё раз", "Try again")) { tries++ }
        list.forEach { st -> StationRow(st, here) { choose(st.lat, st.lon) } }
        if (exit) {
            Spacer(Modifier.height(6.dp))
            Ghost(tr("Закончить без станции", "End without a station"), color = C.mute) {
                scope.launch { g.endTrip(); c.app.stopService(android.content.Intent(c.app, TripService::class.java)) }
                onDismiss()
            }
        }
    }
}

@Composable
private fun StationRow(st: Station, here: Fix?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(st.name, style = T.bodyB)
        }
        here?.let {
            val m = Geo.distance(it.lat, it.lon, st.lat, st.lon)
            Text(if (m < 1000) tr("${m.toInt()} м", "${m.toInt()} m") else tr("${app.leyline.domain.fmt1(m / 1000)} км", "${app.leyline.domain.fmt1(m / 1000)} km"), style = T.small)
        }
    }
}
