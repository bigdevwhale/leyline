package app.leyline.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leyline.container
import app.leyline.data.State
import app.leyline.domain.tr
import kotlinx.coroutines.delay

/** Which sheet is open over the tabs. */
sealed interface Over {
    data object Rides : Over
    data object Settings : Over
    data object Spirits : Over
    data object Omen : Over
    data object Sigil : Over
    data object Journal : Over
    data object Cozy : Over
    class TripPage(val start: Long) : Over
    class Spirit(val id: String) : Over
    class Sanctum(val id: Long) : Over
    class Quarter(val id: String) : Over
    class Ingredient(val id: String) : Over
    class Recipe(val id: String) : Over
    /** By metro: the entrance or the exit station. */
    class Station(val exit: Boolean) : Over
}

/** The tabs, for jumping between them from a sheet. */
object Nav { var tab by mutableIntStateOf(0) }

@Composable
fun Root(s: State) {
    val c = LocalContext.current.container
    var over by remember { mutableStateOf<Over?>(app.leyline.data.Demo.open?.let { o ->
        when {
            o == "rides" -> Over.Rides
            o == "spirits" -> Over.Spirits
            o == "omen" -> Over.Omen
            o == "sigil" -> Over.Sigil
            o == "journal" -> Over.Journal
            o == "cozy" -> Over.Cozy
            o.startsWith("spirit:") -> Over.Spirit(o.substringAfter(':'))
            o.startsWith("recipe:") -> Over.Recipe(o.substringAfter(':'))
            o.startsWith("ing:") -> Over.Ingredient(o.substringAfter(':'))
            o == "trip" -> s.trips.firstOrNull()?.let { Over.TripPage(it.start) }
            o == "sanctum" -> s.sanctums.maxByOrNull { it.visits }?.let { Over.Sanctum(it.id) }
            else -> null
        }
    }) }
    LaunchedEffect(Unit) { if (app.leyline.data.Demo.tab >= 0) Nav.tab = app.leyline.data.Demo.tab }
    val tab = Nav.tab
    val finished by c.game.finished.collectAsState()
    val live by c.game.live.collectAsState()
    val locked = s.trip?.let { app.leyline.domain.Ride.of(it.ride).driver } == true && live.moving
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.game.news.collect { toast = it.text } }
    // the metro's stations are loaded ahead, while there is a network and time: underground there may be neither
    LaunchedEffect(s.homeLat) { if (s.homeLat != 0.0) c.game.stations() }
    LaunchedEffect(toast) { if (toast != null) { delay(2600); toast = null } }
    LaunchedEffect(finished) { finished?.let { over = Over.TripPage(it); Nav.tab = 0; c.game.finished.value = null } }
    BackHandler(tab != 0 && over == null) { Nav.tab = 0 }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        // the screens run under the floating dock; those with their own lists leave room for it (and for the system's buttons under it)
        Box(Modifier.fillMaxSize().then(if (!locked && (tab == 2 || tab == 3)) Modifier.navigationBarsPadding().padding(bottom = Dock.H) else Modifier)) {
            when {
                s.trip != null && tab == 0 -> OnTheWay(s, locked) { over = it }
                tab == 0 -> Tower(s) { over = it }
                tab == 1 -> LeyMap(s) { over = it }
                tab == 2 -> Grimoire(s) { over = it }
                else -> Stable(s) { over = it }
            }
        }
        // a driver in motion sees only the road; the dock waits for the stop
        if (!locked) Dock(tab, Modifier.align(Alignment.BottomCenter), onTrip = s.trip != null, onSel = { Nav.tab = it }) {
            if (s.trip != null) Nav.tab = 0 else over = Over.Rides
        }
        toast?.let {
            Text(it, style = T.smallB.copy(color = Color(0xFFF6FFFD)), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp, start = 24.dp, end = 24.dp)
                    .clip(RoundedCornerShape(12.dp)).background(C.indigo).padding(horizontal = 14.dp, vertical = 8.dp))
        }
        if (!locked) when (val o = over) {
            null -> {}
            Over.Rides -> RidesSheet(s) { over = null }
            Over.Settings -> SettingsSheet(s) { over = null }
            Over.Spirits -> SpiritsSheet(s, onOpen = { over = Over.Spirit(it) }) { over = null }
            Over.Omen -> OmenSheet(s) { over = null }
            Over.Sigil -> SigilSheet(s) { over = null }
            Over.Cozy -> CozySheet(s) { over = null }
            Over.Journal -> JournalSheet(s, onOpen = { over = Over.TripPage(it) }) { over = null }
            is Over.TripPage -> TripSheet(s, o.start, open = { over = it }) { over = null }
            is Over.Spirit -> SpiritSheet(s, o.id) { over = null }
            is Over.Sanctum -> SanctumSheet(s, o.id) { over = null }
            is Over.Quarter -> QuarterSheet(s, o.id, open = { over = it }) { over = null }
            is Over.Ingredient -> IngredientSheet(s, o.id) { over = null }
            is Over.Recipe -> RecipeSheet(s, o.id) { over = null }
            is Over.Station -> StationSheet(s, o.exit) { over = null }
        }
        // each screen introduces itself once; «?» brings it back
        if (!locked) introFor(tab, over, s.trip != null)?.let { i ->
            if (Help.open || i.id !in s.told) IntroSheet(i) { Help.open = false; c.game.write { it.copy(told = it.told + i.id) } }
        }
    }
}

object Dock { val H = 96.dp }

/** The floating dock: four places and, in the middle, the orb that sets off (or brings back the ride). */
@Composable
private fun Dock(sel: Int, modifier: Modifier, onTrip: Boolean, onSel: (Int) -> Unit, onOrb: () -> Unit) {
    val items = listOf("tab_home" to tr("Башня", "Tower"), "tab_map" to tr("Карта", "Map"), "tab_book" to tr("Гримуар", "Grimoire"), "tab_stable" to tr("Звери", "Beasts"))
    val glow = C.glow
    val pulse = bob(1800, 1f)
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp).height(76.dp)) {
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(62.dp)
            .clip(RoundedCornerShape(31.dp)).background(if (C.dark) Color(0xF0251B2E) else Color(0xF2FFF8F0))
            .border(1.dp, Brush.horizontalGradient(listOf(glow.copy(alpha = 0.6f), C.violet.copy(alpha = 0.5f), glow.copy(alpha = 0.6f))), RoundedCornerShape(31.dp)),
            verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, (pic, label) ->
                if (i == 2) Spacer(Modifier.width(78.dp))
                val on = i == sel
                Column(Modifier.weight(1f).fillMaxSize().clickable { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Pic(pic, Modifier.size(if (on) 30.dp else 26.dp), alpha = if (on) 1f else 0.6f)
                    Text(label, style = T.tiny.copy(color = if (on) C.ink else C.mute, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp))
                    Box(Modifier.padding(top = 2.dp).size(width = if (on) 16.dp else 0.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(C.thread))
                }
            }
        }
        // the orb
        Box(Modifier.align(Alignment.TopCenter).size(70.dp).drawBehind {
            drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f + pulse * 0.2f), Color.Transparent)), size.minDimension * 0.62f)
        }.clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFD27A), Color(0xFFF2A65A), Color(0xFFE07A8A)))).border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape).clickable(onClick = onOrb),
            contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().drawBehind { star(Offset(size.width * 0.74f, size.height * 0.24f), 3.dp.toPx(), Color.White, 0.9f); star(Offset(size.width * 0.26f, size.height * 0.7f), 2.dp.toPx(), Color.White, 0.7f) })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (onTrip) Icons.Outlined.NearMe else Icons.Outlined.Route, null, tint = Color(0xFF2A1606), modifier = Modifier.size(26.dp))
                Text(if (onTrip) tr("В пути", "Riding") else tr("В путь", "Set off"), style = T.tiny.copy(color = Color(0xFF2A1606), fontWeight = FontWeight.Bold, fontSize = 11.sp))
            }
        }
    }
}
