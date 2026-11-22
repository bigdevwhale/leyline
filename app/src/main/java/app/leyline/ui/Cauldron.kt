package app.leyline.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.leyline.data.Game
import app.leyline.data.State
import app.leyline.domain.Book
import app.leyline.domain.Decors
import app.leyline.domain.Lab
import app.leyline.domain.fmt
import app.leyline.domain.tr
import kotlinx.coroutines.launch

/**
 * A painted room that fills its box (cropped like ContentScale.Crop) with the cozy things you own standing in it:
 * each is placed by its spot in the picture, so it stays put on any screen.
 */
@Composable
fun Room(pic: String, decor: Set<String>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        Pic(pic, Modifier.fillMaxSize(), ContentScale.Crop)
        // with things in the room the painting steps back a little, so they read in front of it
        if (decor.isNotEmpty()) Box(Modifier.fillMaxSize().background(Color(0x2A140A18)))
        // the picture is 2:3; find where its corners land after the crop
        val w = maxWidth; val h = maxHeight
        val sw = if (w / h > 2f / 3f) w else h * (2f / 3f)
        val sh = sw * 1.5f
        val ox = (w - sw) / 2; val oy = (h - sh) / 2
        Decors.all.filter { it.id in decor }.sortedBy { it.y }.forEach { d ->
            val id = drawableId(d.pic)
            if (id != 0) {
                val p = painterResource(id)
                val dw = sw * d.w
                val dh = dw * (p.intrinsicSize.height / p.intrinsicSize.width)
                val top = oy + sh * d.y - dh
                val left = ox + sw * d.x - dw / 2
                val hanging = d.id in HANGING
                // a soft halo behind what gives light
                if (d.id in GLOWING) Box(Modifier.offset(x = left - dw * 0.35f, y = top - dh * 0.25f).size(dw * 1.7f, dh * 1.5f)
                    .background(Brush.radialGradient(listOf(Color(0x66FFD27A), Color(0x22FFB070), Color.Transparent))))
                // a shadow on the floor under what stands on it
                if (!hanging) Box(Modifier.offset(x = left + dw * 0.08f, y = top + dh - dw * 0.07f).size(dw * 0.84f, dw * 0.14f)
                    .clip(androidx.compose.foundation.shape.CircleShape).background(Brush.radialGradient(listOf(Color(0x99140A12), Color.Transparent))))
                Box(Modifier.offset(x = left, y = top).size(dw, dh)) {
                    // an ink outline: the silhouette laid around the picture, like a sticker's edge
                    val edge = 1.6.dp
                    val ink = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xE61C1020), androidx.compose.ui.graphics.BlendMode.SrcIn)
                    for ((dx, dy) in OUTLINE) Image(p, null, Modifier.fillMaxSize().offset(x = edge * dx, y = edge * dy), colorFilter = ink)
                    Image(p, null, Modifier.fillMaxSize(), colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(LIFT))
                }
            }
        }
    }
}

private val HANGING = setOf("garland", "mobile", "cage")
private val GLOWING = setOf("garland", "lamp", "ball", "cage", "mushrooms", "books")
private val OUTLINE = listOf(1f to 0f, -1f to 0f, 0f to 1f, 0f to -1f, 0.7f to 0.7f, -0.7f to 0.7f, 0.7f to -0.7f, -0.7f to -0.7f)

/** A touch more light and colour for the things, so they stand in front of the painting. */
private val LIFT = androidx.compose.ui.graphics.ColorMatrix().apply {
    setToSaturation(1.18f)
    val k = 1.1f
    timesAssign(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(k, 0f, 0f, 0f, 6f, 0f, k, 0f, 0f, 6f, 0f, 0f, k, 0f, 6f, 0f, 0f, 0f, 1f, 0f)))
}

/** The shop of cozy things: where spare ingredients go. */
@Composable
fun CozySheet(s: State, onDismiss: () -> Unit) {
    val g = LocalGame
    Sheet(onDismiss) {
        Title(tr("Уют башни", "Tower comforts"), tr("Вещей: ${s.decor.size} из ${Decors.all.size}. Каждая добавляет 2% силы за поездку – сейчас +${s.decor.size * 2}%.",
            "Things: ${s.decor.size} of ${Decors.all.size}. Each adds 2% power per ride – now +${s.decor.size * 2}%."))
        Spacer(Modifier.height(8.dp))
        Decors.all.forEach { d ->
            val have = d.id in s.decor
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(C.paper2.copy(alpha = if (have) 0.5f else 1f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Pic(d.pic, Modifier.size(58.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = T.bodyB)
                    if (have) Text(tr("уже в башне", "already in the tower"), style = T.tiny.copy(color = C.teal))
                    else Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        d.needs.forEach { (k, n) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Pic(Book.ing(k).pic, Modifier.size(22.dp), shadow = k !in s.seen)
                                Text("${minOf(s.has(k), n)}/$n", style = T.tiny.copy(color = if (s.has(k) >= n) C.ink else C.mute))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pic("pot_power", Modifier.size(22.dp))
                            Text(fmt(d.power), style = T.tiny.copy(color = if (s.power >= d.power) C.ink else C.mute))
                        }
                    }
                }
                if (!have) Btn(tr("Взять", "Get"), small = true, enabled = g.canBuy(s, d)) { g.buy(d.id) }
            }
        }
    }
}

/**
 * The cauldron: drop in two or three different ingredients and see what happens. Below it the shelf –
 * tap a jar to drop one in, hold it to read where it comes from.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CauldronTab(s: State, open: (Over) -> Unit) {
    val g = LocalGame
    val scope = rememberCoroutineScope()
    val pot = remember { mutableStateListOf<String>() }
    var result by remember { mutableStateOf<Game.Trial?>(null) }
    // what was taken off the shelf is no longer there to drop
    androidx.compose.runtime.LaunchedEffect(s.bag) { pot.removeAll { s.has(it) < 1 } }
    val key = Lab.key(pot)
    val triedDud = pot.size >= 2 && key in s.tried
    val knownHit = if (pot.size >= 2) Lab.match(pot)?.takeIf { it.id in s.known } else null
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(140.dp).fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                val y = bob(1700, 3f)
                Pic(if (pot.isNotEmpty()) "cauldron_brew" else "cauldron_idle", Modifier.fillMaxSize().offset(y = (-y).dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                val r = result
                when {
                    r != null && r.recipe != null && !r.again -> {
                        Text(tr("Получилось!", "It worked!"), style = T.tiny.copy(color = C.teal))
                        Text(r.recipe.name, style = T.h3)
                        Text(tr("Рецепт записан в гримуар.", "The recipe is written into the grimoire."), style = T.tiny)
                    }
                    r != null && r.recipe != null -> Text(tr("Это уже известный рецепт: ${r.recipe.ru}. Варите его во вкладке «Рецепты».", "That's a recipe you know: ${r.recipe.en}. Brew it in «Recipes»."), style = T.small.copy(color = C.ink))
                    r != null -> {
                        Text(if (r.again) tr("Уже пробовали", "Tried before") else tr("Брак: +${Lab.DUD_POWER} силы", "A dud: +${Lab.DUD_POWER} power"), style = T.tiny.copy(color = C.violet))
                        Text(r.dud, style = T.h3)
                        Text(r.hint, style = T.small.copy(color = C.ink))
                    }
                    else -> {
                        Text(tr("Опыт", "Experiment"), style = T.h3)
                        Text(tr("Бросьте в котёл два или три разных ингредиента – по одному каждого. Угадаете состав – откроется рецепт.",
                            "Drop two or three different ingredients in – one of each. Guess a set right and its recipe opens."), style = T.small)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                val id = pot.getOrNull(i)
                Box(Modifier.size(54.dp).clip(CircleShape).background(C.paper2).border(1.dp, if (id != null) C.glow.copy(alpha = 0.8f) else C.line, CircleShape)
                    .clickable(enabled = id != null) { pot.remove(id); result = null }, contentAlignment = Alignment.Center) {
                    if (id != null) Pic(Book.ing(id).pic, Modifier.size(42.dp)) else Text(if (i == 2) "?" else "+", style = T.h3.copy(color = C.mute))
                }
            }
            Spacer(Modifier.weight(1f))
            Btn(when { knownHit != null -> tr("Известно", "Known"); triedDud -> tr("Был брак", "Was a dud"); else -> tr("Варить", "Brew") }, enabled = pot.size >= 2 && knownHit == null && !triedDud) {
                val ids = pot.toList()
                scope.launch { result = g.experiment(ids); pot.clear() }
            }
        }
        val wood = Brush.verticalGradient(listOf(Color(0xFF8A5A34), Color(0xFF5C3A22)))
        Column(Modifier.weight(1f).framed().padding(horizontal = 6.dp, vertical = 4.dp)) {
            Book.ingredients.chunked(6).forEach { row ->
                Box(Modifier.fillMaxWidth().weight(1f).drawBehind {
                    val y = size.height - 12.dp.toPx()
                    drawRoundRect(Color(0x55000000), Offset(0f, y + 3.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    drawRoundRect(wood, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                }) {
                    Row(Modifier.fillMaxSize()) {
                        row.forEach { ing ->
                            val n = s.has(ing.id) - pot.count { it == ing.id }
                            val seen = ing.id in s.seen
                            val inPot = ing.id in pot
                            Column(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp))
                                .combinedClickable(onLongClick = { open(Over.Ingredient(ing.id)) }) {
                                    if (!seen || s.has(ing.id) < 1) open(Over.Ingredient(ing.id))
                                    else if (inPot) { pot.remove(ing.id); result = null }
                                    else if (pot.size < 3) { pot.add(ing.id); result = null }
                                }.padding(bottom = 11.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text(if (seen) "×$n" else "", style = T.tiny.copy(color = if (n > 0) C.ink else C.mute), textAlign = TextAlign.Center)
                                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 2.dp)
                                    .then(if (inPot) Modifier.clip(CircleShape).background(C.glow.copy(alpha = 0.25f)) else Modifier), contentAlignment = Alignment.BottomCenter) {
                                    Pic(ing.pic, Modifier.fillMaxSize().padding(2.dp), alpha = if (!seen) 0.7f else if (s.has(ing.id) > 0) 1f else 0.4f, shadow = !seen)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(tr("Тап – бросить в котёл, долгое нажатие – где искать. Опытов сделано: ${s.tried.size + s.known.size}.", "Tap to drop it in, hold to see where to look. Experiments so far: ${s.tried.size + s.known.size}."),
            style = T.tiny, modifier = Modifier.padding(vertical = 6.dp))
    }
}
