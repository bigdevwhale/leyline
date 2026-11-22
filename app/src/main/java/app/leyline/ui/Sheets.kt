package app.leyline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.leyline.container
import app.leyline.currentLanguage
import app.leyline.data.State
import app.leyline.data.Trip
import app.leyline.domain.Book
import app.leyline.domain.Ley
import app.leyline.domain.Omen
import app.leyline.domain.Ride
import app.leyline.domain.Sanctums
import app.leyline.domain.Sky
import app.leyline.domain.Spirits
import app.leyline.domain.Weather
import app.leyline.domain.bi
import app.leyline.domain.dayOf
import app.leyline.domain.dur
import app.leyline.domain.fmt
import app.leyline.domain.fmt1
import app.leyline.domain.loc
import app.leyline.domain.relDay
import app.leyline.domain.today
import app.leyline.domain.tr
import app.leyline.domain.zone
import app.leyline.setLanguage
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

fun hhmm(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", loc))

@Composable
fun Tabs2(items: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.paper2).padding(3.dp)) {
        items.forEachIndexed { i, t ->
            Text(t, style = T.smallB.copy(color = if (i == sel) C.accInk else C.ink), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (i == sel) C.acc else Color.Transparent).clickable { onSel(i) }.padding(vertical = 9.dp))
        }
    }
}

/** The route of a ride as a glowing thread on night paper. */
@Composable
fun RouteArt(t: Trip, modifier: Modifier) {
    val pts = t.pts
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF22172C))) {
        // faint stars
        val r = app.leyline.domain.Rand(t.start.toInt())
        repeat(40) { drawCircle(Color(0x55FFFFFF), r.range(0.6f, 1.6f).dp.toPx(), Offset(r.next() * size.width, r.next() * size.height)) }
        if (pts.size < 2) return@Canvas
        val lat0 = pts.map { it.first }.average(); val lon0 = pts.map { it.second }.average()
        val xy = pts.map { app.leyline.trip.Geo.local(lat0, lon0, it.first, it.second) }
        val minX = xy.minOf { it.first }; val maxX = xy.maxOf { it.first }; val minY = xy.minOf { it.second }; val maxY = xy.maxOf { it.second }
        val span = maxOf(maxX - minX, maxY - minY, 50.0)
        val pad = 18.dp.toPx()
        val k = minOf(size.width - 2 * pad, size.height - 2 * pad) / span
        val ox = (size.width - (maxX - minX) * k) / 2; val oy = (size.height - (maxY - minY) * k) / 2
        val path = Path()
        xy.forEachIndexed { i, (x, y) ->
            val px = (ox + (x - minX) * k).toFloat(); val py = (size.height - oy - (y - minY) * k).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, Color(0x335FD9CB), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color(0x8845CFC6), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Color(0xFFD8FFFB), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        fun at(i: Int) = xy[i].let { (x, y) -> Offset((ox + (x - minX) * k).toFloat(), (size.height - oy - (y - minY) * k).toFloat()) }
        drawCircle(Color(0xFFE0B65A), 5.dp.toPx(), at(0))
        drawCircle(Color(0xFF45CFC6), 6.dp.toPx(), at(xy.size - 1))
        drawCircle(Color.White, 2.5.dp.toPx(), at(xy.size - 1))
    }
}

/** «What the road wove»: the card after a ride. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripSheet(s: State, start: Long, open: (Over) -> Unit, onDismiss: () -> Unit) {
    val t = s.trips.firstOrNull { it.start == start } ?: run { onDismiss(); return }
    val ride = Ride.of(t.ride)
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(beastPic(s, ride.beast.id), Modifier.size(52.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Что сплела дорога", "What the road wove"), style = T.title)
                Text("${relDay(dayOf(t.start)).replaceFirstChar { it.uppercase() }}, ${hhmm(t.start)} · ${ride.title}", style = T.small)
            }
        }
        Spacer(Modifier.height(10.dp))
        RouteArt(t, Modifier.fillMaxWidth().height(150.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(fmt1(t.km), tr("км", "km"))
            Stat(dur(((t.end - t.start) / 1000).toInt()), tr("в пути", "on the way"))
            Stat(fmt(t.newCells), tr("новых мест", "new places"))
            Stat("+${fmt(t.power)}", tr("силы", "power"))
        }
        Ornament()
        Text(tr("Ингредиенты", "Ingredients"), style = T.h3)
        Spacer(Modifier.height(6.dp))
        if (t.gains.isEmpty() && t.osm == 1) Text(tr("В этот раз дорога была тихой. Зато зверь подрос.", "The road was quiet this time. But your beast has grown."), style = T.small)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            t.gains.entries.sortedByDescending { it.value }.forEach { (k, n) ->
                Column(Modifier.width(76.dp).clip(RoundedCornerShape(12.dp)).background(C.paper2).clickable { open(Over.Ingredient(k)) }.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Pic(Book.ing(k).pic, Modifier.size(40.dp))
                    Text("×$n", style = T.smallB)
                    Text(Book.ing(k).name, style = T.tiny, maxLines = 1)
                }
            }
        }
        when (t.osm) {
            0 -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = C.acc, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(tr("Читаем карту вдоль пути: мосты, парки, храмы…", "Reading the map along the way: bridges, parks, churches…"), style = T.small)
            }
            2 -> Text(tr("Карта пока не ответила – мосты и парки допишем позже сами.", "The map didn't answer yet – bridges and parks will be added later by themselves."), style = T.small.copy(color = C.mute), modifier = Modifier.padding(top = 8.dp))
        }
        val m = s.mounts[ride.beast.id]
        if (m != null) {
            Ornament()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("${m.title} +${fmt1(t.xp)} км", "${m.title} +${fmt1(t.xp)} km"), style = T.bodyB, modifier = Modifier.weight(1f))
                Text(app.leyline.domain.Mounts.stageName(m.stage), style = T.small)
            }
            Spacer(Modifier.height(4.dp))
            Meter(app.leyline.domain.Mounts.progress(m), color = C.teal)
        }
        if (t.found.isNotEmpty() || t.met.isNotEmpty() || t.notes.isNotEmpty() || t.weather.isNotEmpty() || t.night) Ornament()
        t.found.forEach { id ->
            val r = Book.recipe(id)
            Line(r.pic, tr("Новый рецепт: ${r.ru}", "New recipe: ${r.en}")) { open(Over.Recipe(id)) }
        }
        t.met.forEach { id ->
            val sp = Spirits.byId.getValue(id)
            Line(sp.pic, tr("Вас заметил дух: ${sp.ru}", "A spirit noticed you: ${sp.en}")) { open(Over.Spirit(id)) }
        }
        t.notes.forEach { Line("ic_star", bi(it)) }
        val sky = listOfNotNull(
            Weather.title(t.weather).takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() },
            if (t.fullMoon) tr("ночь полнолуния", "a full-moon night") else if (t.night) tr("после заката", "after sunset") else null,
            if (t.twilight) tr("на заре", "at dawn or dusk") else null,
            if (Omen.of(dayOf(t.start)) in t.gains) tr("знак дня: ${Book.ing(Omen.of(dayOf(t.start))).ru.lowercase()} вдвое", "omen: ${Book.ing(Omen.of(dayOf(t.start))).en.lowercase()} doubled") else null,
            if (t.harvest) tr("щедрая дорога: всё вдвое", "generous road: all doubled") else null,
        )
        if (sky.isNotEmpty()) Text(sky.joinToString(" · "), style = T.small, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Stat(v: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, style = T.num)
        Text(label, style = T.tiny)
    }
}

@Composable
fun Line(pic: String, text: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Pic(pic, Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = T.body, modifier = Modifier.weight(1f))
        if (onClick != null) Text("›", style = T.h3.copy(color = C.acc))
    }
}

/** All rides, newest first, with totals at the top. */
@Composable
fun JournalSheet(s: State, onOpen: (Long) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Title(tr("Дневник дорог", "Road journal"))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(fmt(s.trips.size), tr("поездок", "rides"))
            Stat(fmt1(s.trips.sumOf { it.km }), tr("км", "km"))
            Stat(fmt1(Ley.km(s.ley)), tr("км жил", "km of lines"))
            Power(s.power)
        }
        Text(tr("Сила копится от каждого километра. Ею платят за пробуждение зверей, обереги и чудеса.",
            "Power builds up with every kilometre. It pays for awakening beasts, wards and wonders."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        Ornament()
        if (s.trips.isEmpty()) Text(tr("Здесь появится первая поездка. Нажмите «В путь» перед тем, как поедете.", "Your first ride will appear here. Tap «Set off» before you ride."), style = T.body)
        s.trips.take(60).forEach { t ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpen(t.start) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(beastPic(s, t.beast.id), Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${relDay(dayOf(t.start)).replaceFirstChar { it.uppercase() }}, ${hhmm(t.start)} · ${fmt1(t.km)} ${tr("км", "km")}", style = T.bodyB)
                    Text(Ride.of(t.ride).title + (s.quarters[t.quarters.lastOrNull().orEmpty()]?.let { " · ${it.name}" } ?: ""), style = T.small, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) { t.gains.entries.sortedByDescending { it.value }.take(4).forEach { Pic(Book.ing(it.key).pic, Modifier.size(26.dp)) } }
            }
        }
    }
}

/** The omen of the day and the sky: moon phase, sunrise and sunset. */
@Composable
fun OmenSheet(s: State, onDismiss: () -> Unit) {
    val om = Book.ing(Omen.of())
    val now = System.currentTimeMillis()
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(om.pic, Modifier.size(64.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Знак дня", "Omen of the day"), style = T.small)
                Text(Omen.line(), style = T.title)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("Сегодня ${om.ru.lowercase()} приходит вдвое чаще. Где его найти: ${om.howRu.replaceFirstChar { it.lowercase() }}.",
            "Today ${om.en.lowercase()} comes twice as often. Where to find it: ${om.howEn.replaceFirstChar { it.lowercase() }}."), style = T.body)
        Ornament()
        Line("ic_moon", tr("Луна: ${Sky.phaseName(now)}", "Moon: ${Sky.phaseName(now)}") +
            if (Sky.fullMoon(now)) tr(". Ночью лунной пыли вдвое больше!", ". Twice the moon dust tonight!") else "")
        Sky.sun(today(), s.homeLat, s.homeLon)?.let { (a, b) ->
            Line("ic_sun", tr("Восход ${hhmm(a)}, закат ${hhmm(b)}. Роса зари – за 45 минут до и после.", "Sunrise ${hhmm(a)}, sunset ${hhmm(b)}. Dawn dew – 45 minutes either side."))
        }
        app.leyline.domain.Luck.bloom(s.quarters.values.filter { it.visits > 0 }.map { it.id })?.let { (q, ing) ->
            val name = s.quarters[q]?.name.orEmpty()
            Line(Book.ing(ing).pic, tr("Цветущий квартал недели – $name. Проедьте через него: ${Book.ing(ing).ru.lowercase()} ×3, раз в день.",
                "This week's blooming quarter is $name. Ride through it: ${Book.ing(ing).en.lowercase()} ×3, once a day.") +
                if (s.bloomDay == today()) tr(" Сегодня уже собрано.", " Gathered today already.") else "")
        }
        Text(tr("Знак меняется каждую полночь, цветущий квартал – по понедельникам.", "The omen changes every midnight, the blooming quarter on Mondays."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Your sigil: the shape of the places you go most. */
@Composable
fun SigilMark(s: State, modifier: Modifier, glow: Color = Color(0xFF45CFC6)) {
    val pts = remember(s.sanctums) { Sanctums.sigil(s.sanctums) }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 * 0.82f
        drawCircle(glow.copy(alpha = 0.5f), r * 1.12f, c, style = Stroke(1.2.dp.toPx()))
        if (pts.size < 2) { drawCircle(glow, r * 0.18f, c); return@Canvas }
        val p = pts.map { (x, y) -> Offset(c.x + x * r, c.y + y * r) }
        val path = Path().apply { moveTo(p[0].x, p[0].y); p.drop(1).forEach { lineTo(it.x, it.y) }; if (p.size > 2) close() }
        drawPath(path, glow.copy(alpha = 0.3f), style = Stroke(5.dp.toPx(), join = StrokeJoin.Round))
        drawPath(path, glow, style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round))
        // every point joined to the middle: a rune, not a map
        p.forEach { drawLine(glow.copy(alpha = 0.45f), c, it, 1.dp.toPx()) }
        p.forEach { drawCircle(Color(0xFFE0B65A), 2.6.dp.toPx(), it) }
    }
}

@Composable
fun SigilSheet(s: State, onDismiss: () -> Unit) {
    val awake = s.sanctums.filter { it.awake }.sortedByDescending { it.visits }
    Sheet(onDismiss) {
        Title(tr("Ваша печать", "Your sigil"))
        Spacer(Modifier.height(8.dp))
        if (s.mirror) {
            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF22172C)), contentAlignment = Alignment.Center) {
                SigilMark(s, Modifier.size(190.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(tr("Фигура из ваших святилищ – мест, где поездки начинаются и кончаются чаще всего. Она меняется вместе с вашей жизнью.",
                "A figure made of your sanctums – the places where rides start and end most often. It changes as your life does."), style = T.body)
        } else {
            Text(tr("Печать скрыта. Сварите «Зеркало пути» – и увидите фигуру из мест, где вы бываете чаще всего.",
                "The sigil is hidden. Brew the «Mirror of the road» to see the shape of the places you go most."), style = T.body)
            Spacer(Modifier.height(6.dp))
            Line(Book.recipe("mirror").pic, Book.recipe("mirror").name)
        }
        Ornament()
        Text(tr("Святилища: ${awake.size}", "Sanctums: ${awake.size}"), style = T.h3)
        Text(tr("Место пробуждается после трёх поездок, что начались или закончились рядом.", "A place awakens after three rides that started or ended near it."), style = T.small)
        awake.take(6).forEach { sc -> Line(if (sc.ward.isNotEmpty()) Book.recipe(sc.ward).pic else "ic_shrine", "${sc.title(app.leyline.domain.Quarters.at(s.quarters.values, sc.lat, sc.lon)?.name)} · ${sc.visits}") }
    }
}

@Composable
fun SettingsSheet(s: State, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var sure by remember { mutableIntStateOf(0) }
    Sheet(onDismiss) {
        Title(tr("Настройки", "Settings"))
        Spacer(Modifier.height(10.dp))
        Text(tr("Тема", "Theme"), style = T.h3)
        Tabs2(listOf(tr("Как в системе", "System"), tr("Светлая", "Light"), tr("Тёмная", "Dark")), listOf("", "light", "dark").indexOf(s.theme).coerceAtLeast(0)) { i ->
            c.game.write { it.copy(theme = listOf("", "light", "dark")[i]) }
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("Язык", "Language"), style = T.h3)
        Tabs2(listOf(tr("Как в системе", "System"), "Русский", "English"), listOf("", "ru", "en").indexOf(currentLanguage()).coerceAtLeast(0)) { i -> setLanguage(listOf("", "ru", "en")[i]) }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Голос в пути", "Voice on the way"), style = T.bodyB)
                Text(tr("Скажет, когда вы въезжаете в новые земли", "Says when you enter new lands"), style = T.small)
            }
            Switch(s.voice, { v -> c.game.write { it.copy(voice = v) } }, colors = SwitchDefaults.colors(checkedTrackColor = C.acc))
        }
        Spacer(Modifier.height(12.dp))
        CarPicker(s)
        Spacer(Modifier.height(12.dp))
        Btn(tr("Обновить кварталы вокруг", "Reload the quarters around"), Modifier.fillMaxWidth(), color = C.paper2, ink = C.ink, small = true) {
            scope.launch {
                val f = c.game.live.value.fix ?: c.feed.last()
                if (f != null) c.game.settle(f.lat, f.lon, Ride.of(s.ride))
            }
        }
        Spacer(Modifier.height(8.dp))
        Btn(if (sure == 0) tr("Начать заново", "Start over") else tr("Точно? Всё будет стёрто", "Sure? Everything will be erased"), Modifier.fillMaxWidth(), color = if (sure == 0) C.paper2 else C.bad, ink = if (sure == 0) C.bad else Color.White, small = true) {
            if (sure == 0) sure = 1 else scope.launch { c.game.reset(); onDismiss() }
        }
        Spacer(Modifier.height(10.dp))
        Text(tr("Карта – © участники OpenStreetMap, погода – Open-Meteo. Тропы силы ничего не отправляют о вас: всё хранится на телефоне.",
            "Map – © OpenStreetMap contributors, weather – Open-Meteo. Leyline sends nothing about you: everything stays on the phone."), style = T.tiny)
    }
}

/** Which paired Bluetooth device is the car: when it connects, the app offers the ride; when it goes, the ride ends. */
@Composable
fun CarPicker(s: State) {
    val ctx = LocalContext.current
    val c = ctx.container
    var list by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    Text(tr("Машина по Bluetooth", "The car by Bluetooth"), style = T.h3)
    Text(tr("Когда телефон подключится к машине, придёт уведомление «Дрейк готов» – одно нажатие, и поездка идёт. Заглушили мотор – поездка закончилась сама.",
        "When the phone connects to the car a «Your drake is ready» notification comes – one tap and the ride is on. Engine off – the ride ends by itself."), style = T.small)
    Spacer(Modifier.height(6.dp))
    if (s.btAddr.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(beastPic(s, "car"), Modifier.size(40.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.btName, style = T.bodyB, modifier = Modifier.weight(1f))
            Ghost(tr("Убрать", "Remove"), color = C.bad) { c.game.write { it.copy(btAddr = "", btName = "") } }
        }
    } else if (list == null) {
        Btn(tr("Выбрать машину", "Choose the car"), Modifier.fillMaxWidth(), color = C.paper2, ink = C.ink, small = true) {
            val ask = if (android.os.Build.VERSION.SDK_INT >= 31) listOf(android.Manifest.permission.BLUETOOTH_CONNECT) else emptyList()
            app.leyline.MainActivity.ask(ask) { list = app.leyline.trip.Auto.paired(ctx) }
        }
    } else {
        if (list!!.isEmpty()) Text(tr("Нет сопряжённых устройств (или Bluetooth не разрешён). Сначала подключите телефон к машине в настройках Bluetooth.",
            "No paired devices (or Bluetooth isn't allowed). Pair the phone with the car in Bluetooth settings first."), style = T.small.copy(color = C.bad))
        list!!.forEach { (addr, name) ->
            Text(name, style = T.bodyB, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { c.game.write { it.copy(btAddr = addr, btName = name) }; list = null }.padding(vertical = 10.dp, horizontal = 6.dp))
        }
    }
}
