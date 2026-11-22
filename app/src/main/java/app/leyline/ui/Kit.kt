package app.leyline.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.leyline.domain.fmt

fun Modifier.tap(onClick: () -> Unit) = this.clickable(MutableInteractionSource(), null, onClick = onClick)

private val ids = HashMap<String, Int>()

@SuppressLint("DiscouragedApi")
@Composable
fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return ids.getOrPut(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

/** A drawable by name (the cut Codex art). */
@Composable
fun Pic(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, alpha: Float = 1f, shadow: Boolean = false) {
    val id = drawableId(name)
    // shadow: a silhouette of the thing, for what is not found yet
    if (id != 0) Image(painterResource(id), null, modifier.alpha(alpha), contentScale = scale,
        colorFilter = if (shadow) androidx.compose.ui.graphics.ColorFilter.tint(if (C.dark) Color(0xFF5A4868) else Color(0xFF8C7889), androidx.compose.ui.graphics.BlendMode.SrcIn) else null)
    else Box(modifier)
}

/** Smoked glass edged with a thread of light (teal into violet), a brighter glint along the top. */
fun Modifier.framed(r: Dp = 22.dp, fill: Color = C.paper, rule: Color = C.glow) = this
    .clip(RoundedCornerShape(r))
    .background(fill.copy(alpha = if (C.dark) 0.88f else 0.9f))
    .border(1.dp, Brush.linearGradient(listOf(rule.copy(alpha = 0.7f), C.violet.copy(alpha = 0.35f), rule.copy(alpha = 0.15f))), RoundedCornerShape(r))
    .drawBehind {
        // candlelight from above and a glint along the top edge
        drawRect(Brush.verticalGradient(listOf(rule.copy(alpha = if (C.dark) 0.10f else 0.07f), Color.Transparent), endY = size.height * 0.6f))
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, rule.copy(alpha = 0.9f), Color.Transparent)),
            Offset(r.toPx(), 0.6f), Offset(size.width - r.toPx(), 0.6f), 1.5.dp.toPx())
        // two little stars in the corners
        star(Offset(size.width - r.toPx() * 0.7f, r.toPx() * 0.55f), 3.2.dp.toPx(), rule)
        star(Offset(r.toPx() * 0.55f, size.height - r.toPx() * 0.6f), 2.2.dp.toPx(), C.violet)
    }

/** A four-pointed twinkle. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.star(c: Offset, r: Float, col: Color, alpha: Float = 0.85f) {
    val p = Path().apply {
        moveTo(c.x, c.y - r); quadraticTo(c.x, c.y, c.x + r, c.y); quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y); quadraticTo(c.x, c.y, c.x, c.y - r); close()
    }
    drawCircle(col.copy(alpha = alpha * 0.25f), r * 1.4f, c)
    drawPath(p, col.copy(alpha = alpha))
}

/** A pill of light: teal into violet for the main action, plain glass for the rest. */
@Composable
fun Btn(text: String, modifier: Modifier = Modifier, color: Color = C.acc, ink: Color = C.accInk, enabled: Boolean = true, icon: ImageVector? = null, small: Boolean = false, onClick: () -> Unit) {
    val main = color == C.acc
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .then(if (!enabled) Modifier.background(C.line.copy(alpha = 0.6f)) else if (main) Modifier.background(Brush.horizontalGradient(listOf(C.glow, C.violet))) else Modifier.background(color))
            .border(1.dp, Color.White.copy(alpha = if (enabled && main) 0.35f else 0.12f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = if (small) 38.dp else 54.dp)
            .padding(horizontal = if (small) 14.dp else 22.dp, vertical = if (small) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (!enabled) C.mute else if (main) Color(0xFF2A1606) else ink
        if (icon != null) { Icon(icon, null, tint = tint, modifier = Modifier.size(if (small) 18.dp else 22.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = (if (small) T.btn.copy(fontSize = T.btn.fontSize * 0.88f) else T.btn).copy(color = tint), textAlign = TextAlign.Center)
    }
}

@Composable
fun Ghost(text: String, modifier: Modifier = Modifier, color: Color = C.ink, onClick: () -> Unit) {
    Text(text, style = T.smallB.copy(color = color), modifier = modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp))
}

/** Power with its glowing orb. */
@Composable
fun Power(n: Int, style: TextStyle = T.num, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Pic("pot_power", Modifier.size(size))
        Spacer(Modifier.width(4.dp))
        Text(fmt(n), style = style)
    }
}

/** A divider like a small constellation: a fading thread with three stars. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = C.glow) {
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp).height(8.dp).drawBehind {
        val y = size.height / 2; val cx = size.width / 2
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.55f), C.violet.copy(alpha = 0.45f), Color.Transparent)), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        star(Offset(cx, y), 5.dp.toPx(), color)
        star(Offset(cx - 18.dp.toPx(), y), 2.5.dp.toPx(), C.violet)
        star(Offset(cx + 18.dp.toPx(), y), 2.5.dp.toPx(), C.violet)
    })
}

/** A bottom sheet over a dim scrim; scrolls when it has to. */
@Composable
fun Sheet(onDismiss: () -> Unit, scroll: Boolean = true, light: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    val shown = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { shown.targetState = true }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(shown, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(if (light) 0x33000000 else 0x88000000)).tap(onDismiss))
        }
        AnimatedVisibility(shown, Modifier.align(Alignment.BottomCenter), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = if (light) 420.dp else 680.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(C.paper)
                    .border(1.dp, Brush.verticalGradient(listOf(C.glow.copy(alpha = 0.8f), Color.Transparent)), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .drawBehind { drawRect(Brush.verticalGradient(listOf(C.glow.copy(alpha = if (C.dark) 0.10f else 0.08f), Color.Transparent), endY = 160.dp.toPx())) }
                    .tap { }
                    .navigationBarsPadding()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 44.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(C.thread))
                Spacer(Modifier.size(10.dp))
                content()
            }
        }
    }
}

/** A small pill with an icon and a number, for the top of a scene. */
@Composable
fun Pill(pic: String, text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(C.paper.copy(alpha = 0.78f)).border(1.dp, C.glow.copy(alpha = 0.45f), RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pic(pic, Modifier.size(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = T.smallB)
    }
}

@Composable
fun Meter(v: Float, modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") color: Color = C.glow) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.line.copy(alpha = 0.5f))) {
        Box(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.thread))
    }
}

@Composable
fun Title(text: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(text, style = T.title)
        if (sub != null) Text(sub, style = T.small)
    }
}

@Composable
fun Scroll(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), content = content)

/** Fireflies: warm motes of light drifting up through a scene, each on its own slow path. */
@Composable
fun Fireflies(modifier: Modifier, n: Int = 18) {
    if (app.leyline.BuildConfig.SHOTS) return
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "flies")
    val time by t.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(24_000, easing = androidx.compose.animation.core.LinearEasing)), label = "t")
    val seeds = remember(n) { val r = app.leyline.domain.Rand(7); List(n) { floatArrayOf(r.next(), r.next(), r.range(0.6f, 1.4f), r.next(), r.range(1.2f, 2.6f)) } }
    androidx.compose.foundation.Canvas(modifier) {
        val warm = Color(0xFFFFD68A); val rose = Color(0xFFFFB3C0)
        seeds.forEachIndexed { i, (x0, y0, speed, ph, r) ->
            val k = (time * speed + ph) % 1f
            val y = size.height * (1f - ((y0 * 0.4f + k) % 1f))
            val x = size.width * x0 + kotlin.math.sin((k * 6.28f * 2 + ph * 6.28f).toDouble()).toFloat() * 18.dp.toPx()
            val a = kotlin.math.sin((k * 3.14159f).toDouble()).toFloat().coerceAtLeast(0f) * (0.5f + 0.5f * kotlin.math.sin((time * 60 + i).toDouble()).toFloat().let { it * it })
            val c = if (i % 3 == 0) rose else warm
            drawCircle(c.copy(alpha = a * 0.25f), r.dp.toPx() * 3.2f, Offset(x, y))
            drawCircle(c.copy(alpha = a), r.dp.toPx(), Offset(x, y))
        }
    }
}
