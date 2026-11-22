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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.leyline.data.State
import app.leyline.domain.Ask
import app.leyline.domain.Book
import app.leyline.domain.Cat
import app.leyline.domain.Quarters
import app.leyline.domain.Spirits
import app.leyline.domain.tr

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
            Line(Book.ing(sp.ing).pic, Spirits.friendLine(sp))
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
        else tr("Награда: $p силы и ${Book.ing(sp.ing).ru.lowercase()} ×$n", "Reward: $p power and ${Book.ing(sp.ing).en.lowercase()} ×$n"), style = T.small, modifier = Modifier.padding(top = 8.dp))
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
            Text(tr("Каждая поездка, что начинается или кончается здесь, приносит +${sc.wardLevel}: ${Book.ing(wardIngOf(sc.ward)).ru.lowercase()}.",
                "Every ride that starts or ends here brings +${sc.wardLevel} ${Book.ing(wardIngOf(sc.ward)).en.lowercase()}."), style = T.body)
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
        if (q.guessed) Text(tr("Вид квартала пока угадан: карта не ответила. Уточним при следующей поездке.", "The kind is a guess for now: the map didn't answer. It'll be checked on the next ride."), style = T.tiny)
    }
}
