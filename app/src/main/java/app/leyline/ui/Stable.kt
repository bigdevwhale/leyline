package app.leyline.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.leyline.data.State
import app.leyline.domain.Beast
import app.leyline.domain.Book
import app.leyline.domain.Mounts
import app.leyline.domain.Ride
import app.leyline.domain.fmt
import app.leyline.domain.fmt1
import app.leyline.domain.tr

@Composable
fun Stable(s: State, open: (Over) -> Unit) {
    val g = LocalGame
    var sel by remember { mutableStateOf(s.beast) }
    val beast = Beast.of(sel)
    val m = s.mounts[sel]
    Column(Modifier.fillMaxSize()) {
        Banner("banner_stable", tr("Звери", "Beasts"), tr("В конюшне: ${s.mounts.size} из ${Beast.entries.size}", "In the stable: ${s.mounts.size} of ${Beast.entries.size}"))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Beast.entries.forEach { b ->
                val on = b.id == sel
                val have = b.id in s.mounts
                Box(Modifier.size(60.dp).clip(CircleShape).background(if (on) C.indigo else C.paper).border(if (on) 2.dp else 1.dp, if (on) C.glow else C.glow.copy(alpha = 0.25f), CircleShape)
                    .clickable { sel = b.id }, contentAlignment = Alignment.Center) {
                    Pic(if (have) beastPic(s, b.id) else b.pic(0), Modifier.size(50.dp), alpha = if (have) 1f else 0.35f)
                    if (have && s.mounts[b.id]?.let { Mounts.ready(it) } == true) Box(Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(C.teal))
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Fireflies(Modifier.fillMaxSize(), 12)
            Box(Modifier.size(260.dp).background(Brush.radialGradient(listOf(C.glow.copy(alpha = if (s.great) 0.0f else 0.22f), Color.Transparent))))
            if (s.great && m != null) Box(Modifier.size(260.dp).background(Brush.radialGradient(listOf(C.gold.copy(alpha = 0.35f), Color.Transparent))))
            val y = bob()
            Pic(if (m != null) beastPic(s, sel) else beast.pic(0), Modifier.fillMaxSize().padding(12.dp).offset(y = (-y).dp), alpha = if (m != null) 1f else 0.3f)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).framed().padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (m == null) {
                Text(beast.title, style = T.title)
                val ride = Ride.entries.first { it.beast == beast }
                Text(tr("${beast.what.replaceFirstChar { it.uppercase() }}. Вылупится в первой поездке: «${ride.ru}».", "${beast.what.replaceFirstChar { it.uppercase() }}. Hatches on your first ride: «${ride.en}»."), style = T.body)
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.title, style = T.title)
                    Text("${Mounts.stageName(m.stage).replaceFirstChar { it.uppercase() }} · ${beast.what} · ${fmt1(m.km)} ${tr("км", "km")} · ${fmt(m.rides)} ${tr("поездок", "rides")}", style = T.small)
                }
                if (s.beast != sel) Ghost(tr("В башню", "To the tower"), color = C.acc) { g.write { it.copy(beast = sel) } }
            }
            Spacer(Modifier.height(6.dp))
            if (m.stage >= 3) Text(tr("Древний зверь: выше расти некуда.", "An ancient beast: there's no higher stage."), style = T.smallB.copy(color = C.teal))
            else if (Mounts.ready(m)) {
                val p = Mounts.AWAKEN[m.stage]
                val have = (s.stock[p] ?: 0) > 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pic(Book.recipe(p).pic, Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (have) tr("Готов к пробуждению!", "Ready to awaken!") else tr("Готов к пробуждению. Нужно зелье «${Book.recipe(p).ru}».", "Ready to awaken. It needs «${Book.recipe(p).en}»."),
                        style = T.smallB, modifier = Modifier.weight(1f).clickable { open(Over.Recipe(p)) })
                    if (have) Btn(tr("Пробудить", "Awaken"), small = true) { g.awaken(sel) }
                }
            } else {
                Meter(Mounts.progress(m), color = C.teal)
                Text(tr("До пробуждения ещё ${fmt1(Mounts.NEED[m.stage] - m.km)} км. Растёт в поездках: ${Ride.entries.filter { it.beast == beast }.joinToString(", ") { it.ru.lowercase() }}.",
                    "${fmt1(Mounts.NEED[m.stage] - m.km)} km more to awakening. Grows on rides: ${Ride.entries.filter { it.beast == beast }.joinToString(", ") { it.en.lowercase() }}."), style = T.tiny, modifier = Modifier.padding(top = 3.dp))
            }
            Ornament()
            Text(tr("Черты", "Traits"), style = T.h3)
            Mounts.traits.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { tr0 ->
                        val earned = tr0.earned(m)
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (earned) C.teal.copy(alpha = 0.16f) else C.paper2)
                            .clickable { g.news.tryEmit(app.leyline.data.News("${tr0.name}: ${tr0.does}. " + tr("Нужно: ${fmt1(tr0.goal)} ${tr0.unitRu}", "Needs: ${fmt1(tr0.goal)} ${tr0.unitEn}"))) }
                            .padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Pic(Book.ing(tr0.ing).pic, Modifier.size(24.dp), alpha = if (earned) 1f else 0.4f)
                            Text(tr0.name, style = T.tiny.copy(color = if (earned) C.ink else C.mute), textAlign = TextAlign.Center, maxLines = 1)
                            if (!earned) Text("${fmt1(minOf(tr0.have(m), tr0.goal))}/${fmt1(tr0.goal)}", style = T.tiny)
                        }
                    }
                }
            }
        }
    }
}
