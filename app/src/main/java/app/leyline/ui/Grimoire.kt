package app.leyline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.leyline.data.State
import app.leyline.domain.Book
import app.leyline.domain.Cat
import app.leyline.domain.Pages
import app.leyline.domain.Recipe
import app.leyline.domain.Spirits
import app.leyline.domain.fmt
import app.leyline.domain.tr
import kotlinx.coroutines.launch

/** A picture banner with the screen's title over its calm left third. */
@Composable
fun Banner(pic: String, title: String, sub: String, right: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().height(132.dp)) {
        Pic(pic, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to C.bg.copy(alpha = 0.92f), 0.6f to C.bg.copy(alpha = 0.25f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to C.bg)))
        Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(title, style = T.big)
                Text(sub, style = T.small.copy(color = C.ink))
            }
            right()
            Spacer(Modifier.width(8.dp))
            HelpBtn()
        }
    }
}

@Composable
fun Grimoire(s: State, open: (Over) -> Unit) {
    var tab by remember { mutableIntStateOf(if (app.leyline.data.Demo.open == "recipes") 1 else 0) }
    Column(Modifier.fillMaxSize()) {
        Banner("banner_grimoire", tr("Гримуар", "Grimoire"), tr("Рецептов открыто: ${s.known.size} из ${Book.recipes.size}", "Recipes found: ${s.known.size} of ${Book.recipes.size}")) {
            Pill("pot_power", fmt(s.power))
        }
        Box(Modifier.padding(horizontal = 12.dp)) {
            Tabs2(listOf(tr("Котёл", "Cauldron"), tr("Рецепты", "Recipes"), tr("Запас", "Stock") + (s.stock.values.sum().takeIf { it > 0 }?.let { " · $it" } ?: "")), tab) { tab = it }
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> CauldronTab(s, open)
            1 -> Recipes(s, open)
            else -> Stock(s, open)
        }
    }
}

/** The needs of a recipe as little jars with have/need. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Needs(s: State, r: Recipe, big: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        r.needs.forEach { (k, n) ->
            val have = s.has(k)
            val seen = k in s.seen
            Row(Modifier.clip(RoundedCornerShape(50)).background(if (have >= n) C.teal.copy(alpha = 0.18f) else C.paper2).padding(start = 3.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Pic(if (seen) Book.ing(k).pic else "pot_unknown", Modifier.size(if (big) 28.dp else 22.dp))
                Text("${minOf(have, n)}/$n", style = T.tiny.copy(color = if (have >= n) C.ink else C.mute))
            }
        }
        if (r.power > 0) Row(Modifier.clip(RoundedCornerShape(50)).background(if (s.power >= r.power) C.teal.copy(alpha = 0.18f) else C.paper2).padding(start = 3.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Pic("pot_power", Modifier.size(if (big) 28.dp else 22.dp))
            Text(fmt(r.power), style = T.tiny.copy(color = if (s.power >= r.power) C.ink else C.mute))
        }
    }
}

@Composable
private fun Recipes(s: State, open: (Over) -> Unit) {
    val g = LocalGame
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        // a full page gives power once and a lasting favour; every page, the keeper's title
        if (Pages.allFound(s)) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).framed(14.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Pic("ic_star", Modifier.size(34.dp)); Spacer(Modifier.width(10.dp))
            Text(tr("Все рецепты найдены. Вы – ${Pages.keeper.lowercase()}.", "Every recipe found. You are the ${Pages.keeper.lowercase()}."), style = T.bodyB)
        } else Text(tr("За заполненную страницу – ${Pages.SET_POWER} силы и подарок навсегда; за все рецепты – звание «${Pages.keeper}» и ${fmt(Pages.ALL_POWER)} силы.",
            "A full page gives ${Pages.SET_POWER} power and a lasting gift; every recipe – the title «${Pages.keeper}» and ${fmt(Pages.ALL_POWER)} power."), style = T.small, modifier = Modifier.padding(top = 2.dp))
        Cat.entries.forEach { cat ->
            val full = cat in Pages.CATS && Pages.complete(s, cat)
            Row(Modifier.padding(top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(cat.plural + " · " + Book.recipes.count { it.cat == cat && it.id in s.known } + "/" + Book.recipes.count { it.cat == cat }, style = T.h3)
                if (full) { Spacer(Modifier.width(6.dp)); Pic("ic_star", Modifier.size(20.dp)) }
            }
            if (cat in Pages.CATS) Text((if (full) "" else tr("вся страница: ", "the full page: ")) + Pages.perk(cat), style = T.tiny.copy(color = if (full) C.teal else C.mute))
            val hidden = Book.recipes.count { it.cat == cat && it.id !in s.known }
            Book.recipes.filter { it.cat == cat && it.id in s.known }.forEach { r ->
                val known = true
                val done = wonderDone(s, r.id)
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(C.paper).clickable { open(Over.Recipe(r.id)) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Pic(if (known) r.pic else "pot_unknown", Modifier.size(46.dp), alpha = if (known) 1f else 0.6f)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (known) r.name else tr("Неизвестный рецепт", "Unknown recipe"), style = T.bodyB)
                        if (known) Needs(s, r) else Text(tr("Откроется, когда соберёте всё, что в него идёт", "Opens once you've gathered all it needs"), style = T.tiny)
                    }
                    if (known && !done && g.canBrew(s, r.id)) Btn(tr("Сварить", "Brew"), small = true) { scope.launch { g.brew(r.id) } }
                    else if (done) Text(tr("готово", "done"), style = T.smallB.copy(color = C.teal))
                    else (s.stock[r.id] ?: 0).takeIf { it > 0 }?.let { Text("×$it", style = T.smallB) }
                }
            }
            if (hidden > 0) Hidden(hidden)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Hidden(n: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(C.paper.copy(alpha = 0.5f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic("pot_unknown", Modifier.size(34.dp), alpha = 0.6f)
        Spacer(Modifier.width(10.dp))
        Text(tr("Ещё не открыто: $n. Ищите опытами в котле", "Not found yet: $n. Look for them by experiment"), style = T.small, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Stock(s: State, open: (Over) -> Unit) {
    val g = LocalGame
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        if (s.buffs.isNotEmpty()) {
            Text(tr("Действуют сейчас", "At work now"), style = T.h3)
            s.buffs.forEach { (id, n) -> Line(Book.recipe(id).pic, "${Book.recipe(id).name}: " + tr("ещё $n ${app.leyline.domain.plural(n.toLong(), "поездка", "поездки", "поездок")}", "$n more ${if (n == 1) "ride" else "rides"}")) }
            Ornament()
        }
        if (s.stock.isEmpty()) {
            Text(tr("Сваренные зелья ждут здесь. Эликсиры пьют перед поездкой, обереги ставят на святилища, пробуждения – для зверей, дары – духам.",
                "Brewed potions wait here. Elixirs are drunk before a ride, wards go on sanctums, awakenings are for beasts, gifts for spirits."), style = T.body)
        }
        s.stock.entries.sortedBy { Book.recipe(it.key).cat.ordinal }.forEach { (id, n) ->
            val r = Book.recipe(id)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(C.paper).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic(r.pic, Modifier.size(46.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable { open(Over.Recipe(id)) }) {
                    Text("${r.name} ×$n", style = T.bodyB)
                    Text(r.does, style = T.tiny)
                }
                when (r.cat) {
                    Cat.ELIXIR -> Btn(tr("Выпить", "Drink"), small = true) { g.drink(id) }
                    Cat.AWAKEN -> Btn(tr("К зверям", "Beasts"), small = true, color = C.indigo, ink = Color.White) { Nav.tab = 3 }
                    Cat.WARD -> Btn(tr("На карту", "Map"), small = true, color = C.indigo, ink = Color.White) { Nav.tab = 1 }
                    Cat.GIFT -> Btn(tr("Духу", "Spirit"), small = true, color = C.indigo, ink = Color.White) { open(Over.Spirit(id.removePrefix("g_"))) }
                    Cat.WONDER -> {}
                }
            }
        }
        val wonders = listOfNotNull("aurora".takeIf { s.aurora }, "mirror".takeIf { s.mirror }, "great".takeIf { s.great })
        if (wonders.isNotEmpty()) {
            Ornament()
            Text(tr("Чудеса", "Wonders"), style = T.h3)
            wonders.forEach { Line(Book.recipe(it).pic, Book.recipe(it).name + " – " + Book.recipe(it).does) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun RecipeSheet(s: State, id: String, onDismiss: () -> Unit) {
    val r = Book.recipe(id)
    val g = LocalGame
    val scope = rememberCoroutineScope()
    val known = id in s.known
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(if (known) r.pic else "pot_unknown", Modifier.size(84.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.cat.title, style = T.small)
                Text(if (known) r.name else tr("Неизвестный рецепт", "Unknown recipe"), style = T.title)
                val have = s.stock[id] ?: 0
                if (have > 0) Text(tr("В запасе: $have", "In stock: $have"), style = T.smallB.copy(color = C.teal))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (known) {
            Text(r.does, style = T.body)
            Ornament()
            Text(tr("Что нужно", "What it takes"), style = T.h3)
            Spacer(Modifier.height(4.dp))
            r.needs.forEach { (k, n) ->
                val ing = Book.ing(k)
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pic(ing.pic, Modifier.size(34.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${ing.name}: ${minOf(s.has(k), n)}/$n", style = T.bodyB.copy(color = if (s.has(k) >= n) C.ink else C.ink))
                        if (s.has(k) < n) Text(ing.how, style = T.tiny)
                    }
                    if (s.has(k) >= n) Text("✓", style = T.h3.copy(color = C.teal))
                }
            }
            if (r.power > 0) Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Pic("pot_power", Modifier.size(34.dp)); Spacer(Modifier.width(8.dp))
                Text(tr("Сила: ${fmt(minOf(s.power, r.power))}/${fmt(r.power)}", "Power: ${fmt(minOf(s.power, r.power))}/${fmt(r.power)}"), style = T.bodyB, modifier = Modifier.weight(1f))
                if (s.power >= r.power) Text("✓", style = T.h3.copy(color = C.teal))
            }
            Spacer(Modifier.height(10.dp))
            if (wonderDone(s, id)) Text(tr("Это чудо уже сотворено.", "This wonder is already done."), style = T.bodyB.copy(color = C.teal))
            else Btn(tr("Сварить", "Brew"), Modifier.fillMaxWidth(), enabled = g.canBrew(s, id)) { scope.launch { g.brew(id) } }
        } else {
            Text(tr("Эта страница гримуара пуста. Рецепты находят опытом: бросьте в котёл два или три разных ингредиента и посмотрите, что выйдет. Подарки духам духи подсказывают сами.",
                "This page of the grimoire is empty. Recipes are found by experiment: drop two or three different ingredients into the cauldron and see what comes out. Spirits tell you their own gifts."), style = T.body)
        }
    }
}

@Composable
fun IngredientSheet(s: State, id: String, onDismiss: () -> Unit) {
    val ing = Book.ing(id)
    val seen = id in s.seen
    val g = LocalGame
    val scope = rememberCoroutineScope()
    Sheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pic(if (seen) ing.pic else "pot_unknown", Modifier.size(84.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (seen) ing.name else tr("Ещё не найдено", "Not found yet"), style = T.title)
                Text(tr("На полке: ${s.has(id)}", "On the shelf: ${s.has(id)}"), style = T.small)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("Где найти", "Where to find"), style = T.h3)
        Text(ing.how, style = T.body)
        Spirits.all.firstOrNull { it.ing == id }?.let { sp -> Text(tr("Им богаты кварталы вида «${sp.kind.ru}». ", "Quarters of the «${sp.kind.en}» kind are rich in it.") , style = T.small, modifier = Modifier.padding(top = 4.dp)) }
        if (app.leyline.domain.Omen.of() == id) Text(tr("Сегодня это знак дня: его вдвое больше!", "It's today's omen: twice as much!"), style = T.smallB.copy(color = C.teal), modifier = Modifier.padding(top = 4.dp))
        // the sky's ingredients: where the weather never brings them, the cauldron distils them
        if (id in Book.WEATHER) {
            val from = Book.distilFrom(s.bag, id)
            Text(tr("Нет такой погоды? Котёл перегонит ${Book.DISTIL} одинаковых ингредиента в один такой.",
                "No such weather where you live? The cauldron distils ${Book.DISTIL} of one ingredient into one of these."), style = T.small, modifier = Modifier.padding(top = 8.dp))
            Btn(if (from != null) tr("Перегнать ${Book.DISTIL} × ${Book.ing(from).ru}", "Distil ${Book.DISTIL} × ${Book.ing(from).en}")
                else tr("Нужно ${Book.DISTIL} одинаковых", "Needs ${Book.DISTIL} of one kind"),
                Modifier.fillMaxWidth().padding(top = 6.dp), enabled = from != null, small = true) { scope.launch { g.distil(id) } }
        }
        Ornament()
        Text(tr("Идёт в зелья", "Goes into"), style = T.h3)
        val uses = Book.recipes.filter { id in it.needs && it.id != "great" }
        uses.filter { it.id in s.known }.forEach { r -> Line(r.pic, r.name) }
        val unknown = uses.count { it.id !in s.known }
        if (unknown > 0) Line("pot_unknown", tr("Ещё рецептов с ним: $unknown – найдите их в котле", "More recipes with it: $unknown – find them in the cauldron"))
    }
}
