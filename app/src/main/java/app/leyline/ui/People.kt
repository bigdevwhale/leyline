package app.leyline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.leyline.data.Errand
import app.leyline.data.State
import app.leyline.domain.Ask
import app.leyline.domain.Book
import app.leyline.domain.Cat
import app.leyline.domain.Folk
import app.leyline.domain.Kind
import app.leyline.domain.Pages
import app.leyline.domain.Quarters
import app.leyline.domain.Spirits
import app.leyline.domain.fmt
import app.leyline.domain.tr
import kotlinx.coroutines.launch

@Composable
fun SpiritsSheet(s: State, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    val g = LocalGame
    Sheet(onDismiss) {
        Title(tr("Духи кварталов", "Spirits of the quarters"), tr("У каждого вида кварталов свой дух. Он замечает вас, когда вы впервые проезжаете через его квартал.",
            "Each kind of quarter has its spirit. It notices you the first time you ride through one of its quarters."))
        Spacer(Modifier.height(8.dp))
        Spirits.all.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sp ->
                    val step = s.spirits[sp.id]
                    val ready = g.spiritReady(s, sp.id)
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (ready) C.teal.copy(alpha = 0.18f) else C.paper2)
                        .then(if (step != null) Modifier.clickable { onOpen(sp.id) } else Modifier).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pic(sp.pic, Modifier.size(46.dp), alpha = if (step != null) 1f else 0.3f)
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (step != null) sp.name else "???", style = T.smallB, maxLines = 2)
                            Text(when {
                                step == null -> sp.kind.title
                                step >= 4 -> tr("друг", "friend")
                                ready -> tr("ждёт вас!", "waiting!")
                                else -> tr("просьба ${step + 1} из 4", "request ${step + 1} of 4")
                            }, style = T.tiny.copy(color = if (ready || (step ?: 0) >= 4) C.teal else C.mute))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpiritSheet(s: State, id: String, onDismiss: () -> Unit) {
    val g = LocalGame
    val sp = Spirits.byId[id] ?: run { onDismiss(); return }
    val step = s.spirits[id]
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(sp.pic, Modifier.size(96.dp), alpha = if (step != null) 1f else 0.35f)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sp.name, style = T.title)
                Text(tr("Дух кварталов «${sp.kind.ru}»", "Spirit of the «${sp.kind.en}» quarters"), style = T.small)
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { i -> Box(Modifier.size(width = 22.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if ((step ?: 0) > i) C.teal else C.line)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (step == null) {
            Text(tr("Вы ещё не встречались. Проедьте через любой квартал вида «${sp.kind.ru}» – и дух вас заметит.", "You haven't met yet. Ride through any «${sp.kind.en}» quarter and the spirit will notice you."), style = T.body)
            return@Sheet
        }
        Text("«${sp.greet}»", style = T.body.copy(fontFamily = Phil))
        Ornament()
        if (step >= 4) {
            Text(tr("Вы друзья", "You are friends"), style = T.h3)
            Line(Book.ing(sp.ing).pic, Spirits.friendLine(sp, Spirits.friendGift(s)))
            return@Sheet
        }
        val st = sp.steps[step]
        Text(tr("Просьба ${step + 1} из 4", "Request ${step + 1} of 4"), style = T.small)
        Text("«${st.say}»", style = T.h3)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.paper2).padding(10.dp)) {
            Text(tr("Как выполнить", "How to do it"), style = T.smallB)
            Text(Spirits.how(sp, st), style = T.small.copy(color = C.ink))
            val progress = when (st.ask) {
                Ask.PASS, Ask.RIDE -> tr("Сделано: ${minOf(s.spiritCount[id] ?: 0, st.n)} из ${st.n}", "Done: ${minOf(s.spiritCount[id] ?: 0, st.n)} of ${st.n}")
                Ask.GIVE -> tr("На полке: ${s.has(st.what)} из ${st.n}", "On the shelf: ${s.has(st.what)} of ${st.n}")
                Ask.GIFT -> tr("В запасе: ${s.stock[sp.gift] ?: 0}", "In stock: ${s.stock[sp.gift] ?: 0}")
            }
            Text(progress, style = T.smallB.copy(color = C.teal), modifier = Modifier.padding(top = 4.dp))
            if (st.ask == Ask.PASS || st.ask == Ask.RIDE) Text(tr("Считаются поездки после того, как дух попросил.", "Rides after the spirit asked count."), style = T.tiny)
        }
        val (p, n) = Spirits.reward(step)
        Text(if (step == 3) tr("Награда: дружба навсегда, $p силы и ${Book.ing(sp.ing).ru.lowercase()} ×$n", "Reward: friendship for good, $p power and ${Book.ing(sp.ing).en.lowercase()} ×$n")
        else tr("Награда: $p силы, ${Book.ing(sp.ing).ru.lowercase()} ×$n и подсказка к рецепту", "Reward: $p power, ${Book.ing(sp.ing).en.lowercase()} ×$n and a clue to a recipe"), style = T.small, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(10.dp))
        val ready = g.spiritReady(s, id)
        Btn(when (st.ask) { Ask.GIVE, Ask.GIFT -> tr("Отдать", "Give"); else -> tr("Забрать награду", "Take the reward") }, Modifier.fillMaxWidth(), enabled = ready) { g.spiritDone(id) }
    }
}

@Composable
fun SanctumSheet(s: State, id: Long, onDismiss: () -> Unit) {
    val g = LocalGame
    val sc = s.sanctums.firstOrNull { it.id == id } ?: run { onDismiss(); return }
    val qName = Quarters.at(s.quarters.values, sc.lat, sc.lon)?.name
    var name by remember { mutableStateOf(sc.name) }
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(if (sc.ward.isNotEmpty()) Book.recipe(sc.ward).pic else "ic_shrine", Modifier.size(72.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sc.title(qName), style = T.title)
                Text(tr("Поездок отсюда и сюда: ${sc.visits}", "Rides from and to here: ${sc.visits}"), style = T.small)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("Название", "Name"), style = T.smallB)
        BasicTextField(name, { name = it.take(30) }, singleLine = true, textStyle = T.body, cursorBrush = SolidColor(C.acc),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(C.paper2).border(1.dp, C.line, RoundedCornerShape(10.dp)).padding(10.dp),
            decorationBox = { inner -> if (name.isEmpty()) Text(tr("Например: Дом, Работа, У мамы", "E.g. Home, Work, Mum's"), style = T.body.copy(color = C.mute)); inner() })
        if (name != sc.name) Ghost(tr("Сохранить", "Save"), color = C.acc) { g.rename(id, name) }
        Ornament()
        if (sc.ward.isNotEmpty()) {
            val r = Book.recipe(sc.ward)
            Text(tr("Оберег: ${r.ru}, уровень ${sc.wardLevel} из 3", "Ward: ${r.en}, level ${sc.wardLevel} of 3"), style = T.h3)
            val n = sc.wardLevel + if (Pages.complete(s, Cat.WARD)) 1 else 0
            Text(tr("Каждая поездка, что начинается или кончается здесь, приносит +$n: ${Book.ing(wardIngOf(sc.ward)).ru.lowercase()}.",
                "Every ride that starts or ends here brings +$n ${Book.ing(wardIngOf(sc.ward)).en.lowercase()}."), style = T.body)
            Spacer(Modifier.height(6.dp))
        } else {
            Text(tr("Сюда можно поставить оберег: его варят в гримуаре. Оберег даёт свой ингредиент в каждой поездке, что начинается или кончается здесь.",
                "A ward can stand here: they're brewed in the grimoire. It gives its ingredient on every ride that starts or ends here."), style = T.body)
        }
        val wards = s.stock.filterKeys { Book.recipe[it]?.cat == Cat.WARD }
        if (wards.isEmpty()) Text(tr("В запасе оберегов нет.", "No wards in stock."), style = T.small)
        wards.forEach { (w, n) ->
            val r = Book.recipe(w)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(r.pic, Modifier.size(40.dp))
                Spacer(Modifier.width(8.dp))
                Text("${r.name} ×$n", style = T.bodyB, modifier = Modifier.weight(1f))
                val label = when { sc.ward == w && sc.wardLevel >= 3 -> null; sc.ward == w -> tr("Усилить", "Raise"); sc.ward.isNotEmpty() -> tr("Заменить", "Replace"); else -> tr("Поставить", "Place") }
                if (label != null) Btn(label, small = true) { g.ward(id, w) }
            }
        }
        // a stray place – a one-off taxi stop, an old job – can be let go; riding from it again starts it anew
        var sure by remember(id) { mutableStateOf(false) }
        Ornament()
        if (!sure) Ghost(tr("Забыть это место", "Forget this place"), color = C.mute) { sure = true }
        else {
            Text(tr("Святилище погаснет и пропадёт с карты" + (if (sc.ward.isNotEmpty()) ", а с ним и оберег." else ".") + " Если поездки снова будут начинаться или кончаться здесь – место пробудится заново.",
                "The sanctum will go out and leave the map" + (if (sc.ward.isNotEmpty()) ", and its ward with it." else ".") + " If rides start or end here again, the place will awaken anew."), style = T.small)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(tr("Забыть", "Forget"), Modifier.weight(1f), color = C.bad, ink = Color.White, small = true) { g.forget(id); onDismiss() }
                Btn(tr("Оставить", "Keep it"), Modifier.weight(1f), color = C.paper2, ink = C.ink, small = true) { sure = false }
            }
        }
    }
}

fun wardIngOf(w: String) = when (w) { "w_grove" -> "resin"; "w_well" -> "mist"; "w_lantern" -> "moon"; "w_menhir" -> "stone"; "w_bell" -> "wax"; else -> "shard" }

@Composable
fun QuarterSheet(s: State, id: String, open: (Over) -> Unit, onDismiss: () -> Unit) {
    val q = s.quarters[id] ?: run { onDismiss(); return }
    Sheet(onDismiss) {
        Title(q.name, if (q.visits > 0) tr("Проезжали: ${q.visits} раз", "Ridden through: ${q.visits} times") else tr("Вы здесь ещё не ездили", "You haven't ridden here yet"))
        Spacer(Modifier.height(8.dp))
        val k = q.kind
        if (k == null || q.visits == 0) {
            Text(tr("Каким окажется этот квартал и чей дух его стережёт – узнаете, когда проедете через него.", "What this quarter is and whose spirit guards it, you'll learn when you ride through it."), style = T.body)
            return@Sheet
        }
        val sp = Spirits.of(k)
        Line(sp.pic, tr("${k.ru}. Хранитель – ${sp.ru}", "${k.en}. Guardian: ${sp.en}")) { open(Over.Spirit(sp.id)) }
        Line(Book.ing(sp.ing).pic, tr("Им здесь богата дорога: ${Book.ing(sp.ing).ru.lowercase()}", "The road here is rich in ${Book.ing(sp.ing).en.lowercase()}"))
        if (q.guessed && q.changes == 0) Text(tr("Вид квартала пока угадан: карта не ответила. Уточним при следующей поездке.", "The kind is a guess for now: the map didn't answer. It'll be checked on the next ride."), style = T.tiny)
        // the quarter's resident and their errand
        Folk.at(s, q.id)?.let { e ->
            val who = Folk.of(e)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.paper2).clickable { open(Over.Folk(q.id)) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(Folk.pic(e), Modifier.size(38.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Житель: ", "Resident: ") + who.name + ", " + who.role, style = T.bodyB)
                    Text(folkState(s, e), style = T.small)
                }
                Text(if (Folk.ready(s, e)) "!" else "›", style = T.title.copy(color = C.acc))
            }
        }
        // the map may get a quarter wrong – lawns everywhere made «gardens» of half a city: you can name it yourself
        val g = LocalGame
        val scope = rememberCoroutineScope()
        var picking by remember(q.id) { mutableStateOf(false) }
        val price = Quarters.rekindPrice(q)
        Ornament()
        Text(tr("Сменить вид квартала", "Change the quarter's kind") + " · " + (if (price == 0) tr("первый раз бесплатно", "free the first time") else tr("${fmt(price)} силы", "${fmt(price)} power")),
            style = T.smallB.copy(color = if (picking) C.acc else C.mute),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { picking = !picking }.padding(vertical = 6.dp))
        if (picking) {
            Text(tr("Каким вы видите этот квартал? Вместе с видом сменится и его дух-хранитель.", "What is this quarter to you? Its guardian spirit changes with its kind."), style = T.small)
            Kind.entries.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { kk ->
                        val cur = kk == q.kind
                        val can = !cur && s.power >= price
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (cur) C.teal.copy(alpha = 0.2f) else C.paper2)
                            .clickable(enabled = can) { scope.launch { if (g.rekind(q.id, kk)) picking = false } }.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Pic(Spirits.of(kk).pic, Modifier.size(36.dp), alpha = if (can || cur) 1f else 0.4f)
                            Text(kk.title, style = T.tiny, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (price > s.power) Text(tr("Не хватает силы.", "Not enough power."), style = T.tiny.copy(color = C.bad))
        }
    }
}

/** A line on how a resident's story stands. */
fun folkState(s: State, e: Errand): String = when {
    Folk.done(e) -> tr("история окончена – в книге благодарностей", "the story is done – in the book of thanks")
    Folk.ready(s, e) -> tr("можно отчитаться!", "ready to report!")
    e.since > 0 -> tr("просьба ${e.step + 1} из 3 · ", "request ${e.step + 1} of 3 · ") + Folk.progress(s, e).let { (a, b) -> "$a/$b" }
    else -> tr("ждёт с просьбой ${e.step + 1} из 3", "waits with request ${e.step + 1} of 3")
}

/** A resident: their words, the current request, taking it and reporting back. */
@Composable
fun FolkSheet(s: State, quarter: String, onDismiss: () -> Unit) {
    val g = LocalGame
    val scope = rememberCoroutineScope()
    val e = Folk.at(s, quarter) ?: run { onDismiss(); return }
    val who = Folk.of(e)
    val q = s.quarters[quarter]
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(q?.kind?.let { Spirits.of(it).pic } ?: "ic_lantern", Modifier.size(64.dp), alpha = 0.9f)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(who.name, style = T.title)
                Text(who.role.replaceFirstChar { it.uppercase() } + " · " + q?.name.orEmpty(), style = T.small)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("«" + (if (Folk.done(e)) who.thanks else who.hello) + "»", style = T.body.copy(fontFamily = Phil, fontStyle = FontStyle.Italic))
        Ornament()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            who.steps.indices.forEach { i -> Box(Modifier.size(width = 22.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < e.step) C.teal else C.line)) }
            Spacer(Modifier.width(8.dp))
            Text(if (Folk.done(e)) tr("история окончена", "the story is done") else tr("просьба ${e.step + 1} из 3 · без срока", "request ${e.step + 1} of 3 · no deadline"), style = T.smallB)
        }
        Spacer(Modifier.height(8.dp))
        if (Folk.done(e)) {
            Text(tr("Вы помогли до конца. Житель записан в книгу благодарностей.", "You helped to the end. The resident is in the book of thanks."), style = T.body)
            return@Sheet
        }
        val i = e.step
        val reward = tr("Награда: ${Folk.power[i]} силы", "Reward: ${Folk.power[i]} power") +
            (Folk.gift(s, e)?.let { tr(" и ${Book.ing(it).ru.lowercase()} ×${Folk.gifts[i]}", " and ${Book.ing(it).en.lowercase()} ×${Folk.gifts[i]}") } ?: "") +
            (if (i == who.steps.size - 1) tr(", а в конце истории – подсказка к рецепту", ", and at the story's end a clue to a recipe") else "")
        Text("«${Folk.text(s, e)}»", style = T.h3)
        Spacer(Modifier.height(6.dp))
        if (e.since == 0L) {
            Text(reward, style = T.small)
            Spacer(Modifier.height(10.dp))
            val full = Folk.taken(s) >= Folk.MAX_TAKEN
            Btn(tr("Взяться", "Take it on"), Modifier.fillMaxWidth(), enabled = !full) { scope.launch { g.takeErrand(quarter) } }
            if (full) Text(tr("Сразу можно вести не больше ${Folk.MAX_TAKEN} просьб жителей.", "You can carry at most ${Folk.MAX_TAKEN} residents' errands at once."), style = T.tiny.copy(color = C.bad), modifier = Modifier.padding(top = 4.dp))
            return@Sheet
        }
        val (a, b) = Folk.progress(s, e)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.paper2).padding(10.dp)) {
            Text(tr("Как выполнить", "How to do it"), style = T.smallB)
            Text(Folk.how(s, e), style = T.small.copy(color = C.ink))
            Text(tr("Сделано: $a из $b", "Done: $a of $b"), style = T.smallB.copy(color = C.teal), modifier = Modifier.padding(top = 4.dp))
        }
        Text(reward, style = T.small, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(10.dp))
        val ready = Folk.ready(s, e)
        if (a >= b && !ready) Text(tr("Всё готово – осталось приехать сюда, в «${q?.ru}».", "All done – now just come here, to «${q?.en}»."), style = T.small.copy(color = C.acc), modifier = Modifier.padding(bottom = 6.dp))
        Btn(tr("Отчитаться", "Report back"), Modifier.fillMaxWidth(), enabled = ready) { scope.launch { g.claimErrand(quarter) } }
    }
}

/** All residents met, and the book of thanks. */
@Composable
fun FolkListSheet(s: State, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Title(tr("Жители", "Residents"), tr("Появляются в кварталах, где вы бываете часто (от ${Folk.VISITS} поездок). Их просьбы – без срока.",
            "They turn up in quarters you ride through often (${Folk.VISITS} rides or more). Their requests have no deadline."))
        Spacer(Modifier.height(8.dp))
        val (done, open) = s.folk.partition { Folk.done(it) }
        open.sortedByDescending { (if (Folk.ready(s, it)) 2 else 0) + (if (it.since > 0) 1 else 0) }.forEach { e ->
            val who = Folk.of(e)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(if (Folk.ready(s, e)) C.teal.copy(alpha = 0.18f) else C.paper2)
                .clickable { onOpen(e.quarter) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(Folk.pic(e), Modifier.size(38.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(who.name + ", " + who.role + " · " + s.quarters[e.quarter]?.name.orEmpty(), style = T.bodyB, maxLines = 2)
                    Text(folkState(s, e), style = T.small)
                }
                if (Folk.ready(s, e)) Text("!", style = T.title.copy(color = C.acc))
            }
        }
        val left = Folk.all.size - s.folk.size
        if (left > 0) Text(tr("Ещё $left – в кварталах, где вы пока бывали редко.", "$left more – in quarters you've rarely been to yet."), style = T.small, modifier = Modifier.padding(top = 6.dp))
        if (done.isNotEmpty()) {
            Ornament()
            Text(tr("Книга благодарностей", "The book of thanks"), style = T.h3)
            done.forEach { e ->
                val who = Folk.of(e)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Pic("ic_star", Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(who.name + " · " + s.quarters[e.quarter]?.name.orEmpty(), style = T.bodyB)
                        Text("«" + who.thanks + "»", style = T.small.copy(fontStyle = FontStyle.Italic))
                    }
                }
            }
        }
    }
}

