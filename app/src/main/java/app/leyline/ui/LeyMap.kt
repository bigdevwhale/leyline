package app.leyline.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.leyline.BuildConfig
import app.leyline.R
import app.leyline.container
import app.leyline.data.State
import app.leyline.domain.Ley
import app.leyline.domain.Quarters
import app.leyline.domain.fmt1
import app.leyline.domain.tr
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/** The city's hidden net over the real map: lit threads, knots, sanctums with their wards and the spirits of the quarters. */
@Composable
fun LeyMap(s: State, open: (Over) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val live by c.game.live.collectAsState()
    val overlay = remember { LeyOverlay(ctx.applicationContext, onSanctum = { open(Over.Sanctum(it)) }, onQuarter = { open(Over.Quarter(it)) }) }
    val map = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 9.0; maxZoomLevel = 18.0
            isHorizontalMapRepetitionEnabled = false; isVerticalMapRepetitionEnabled = false
            overlays.add(overlay)
            val f = c.game.live.value.fix
            controller.setZoom(13.5)
            controller.setCenter(GeoPoint(f?.lat ?: s.homeLat, f?.lon ?: s.homeLon))
        }
    }
    val dark = C.dark
    LaunchedEffect(dark) {
        // the net glows best on night paper: the tiles are always dimmed, more so in the dark theme
        map.overlayManager.tilesOverlay.setColorFilter(if (dark) candleTiles() else sepiaTiles())
        map.overlayManager.tilesOverlay.loadingBackgroundColor = if (dark) 0xFF1E1524.toInt() else 0xFFE8D9C6.toInt()
    }
    overlay.cells = remember(s.quarters.keys) { app.leyline.domain.Voronoi.cells(s.quarters.values) }; overlay.dark = dark
    overlay.state = s; overlay.fix = live.fix?.let { it.lat to it.lon }
    LaunchedEffect(s, live.fix) { map.invalidate() }
    var centred by remember { mutableStateOf(false) }
    LaunchedEffect(live.fix != null) {
        val f = live.fix ?: return@LaunchedEffect
        if (!centred) { centred = true; map.controller.animateTo(GeoPoint(f.lat, f.lon), 14.5, 600L) }
    }
    DisposableEffect(Unit) {
        val held = !BuildConfig.DEMO
        if (held) c.feed.acquire()
        onDispose { if (held) c.feed.release() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> when (e) { Lifecycle.Event.ON_RESUME -> map.onResume(); Lifecycle.Event.ON_PAUSE -> map.onPause(); else -> Unit } }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); map.onPause(); map.onDetach() }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView({ map }, Modifier.fillMaxSize())
        val knots = remember(s.ley.size) { Ley.knots(s.ley).size }
        Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).framed(14.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pic("tab_map", Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(tr("Жилы: ${fmt1(Ley.km(s.ley))} км · узлов: $knots", "Lines: ${fmt1(Ley.km(s.ley))} km · knots: $knots"), style = T.smallB)
                Text(tr("Святилищ: ${s.sanctums.count { it.awake }} · тапните квартал или святилище", "Sanctums: ${s.sanctums.count { it.awake }} · tap a quarter or a sanctum"), style = T.tiny)
            }
        }
        HelpBtn(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp))
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = Dock.H + 12.dp).navigationBarsPadding().size(54.dp).clip(CircleShape)
            .background(C.paper).border(1.dp, C.glow.copy(alpha = 0.6f), CircleShape)
            .clickable {
                if (!c.feed.permitted()) { app.leyline.Manifests.askLocation { c.feed.restart() }; return@clickable }
                val f = live.fix ?: c.feed.last()
                val p = f?.let { GeoPoint(it.lat, it.lon) } ?: GeoPoint(s.homeLat, s.homeLon)
                map.controller.animateTo(p, 15.0, 600L)
            }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.MyLocation, tr("Где я", "Where am I"), tint = C.acc, modifier = Modifier.size(26.dp))
        }
        if (BuildConfig.DEBUG && !BuildConfig.DEMO) Text("debug: long-press to drive", style = T.tiny, modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = Dock.H + 12.dp).navigationBarsPadding())
    }
    if (BuildConfig.DEBUG && !BuildConfig.DEMO) overlay.onLong = { la, lo -> c.scope.launch { app.leyline.data.Sim.driveTo(c.game, la, lo) } }
}

class LeyOverlay(private val ctx: Context, private val onSanctum: (Long) -> Unit, private val onQuarter: (String) -> Unit) : Overlay() {
    var state: State? = null
    var fix: Pair<Double, Double>? = null
    var cells: Map<String, List<Pair<Double, Double>>> = emptyMap()
    var dark = true
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(3f, 6f), 0f) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    var onLong: ((Double, Double) -> Unit)? = null
    private val bmps = HashMap<String, Bitmap?>()
    private val d = ctx.resources.displayMetrics.density
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; maskFilter = BlurMaskFilter(6 * d, BlurMaskFilter.Blur.NORMAL) }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0xCCB48CFF.toInt(); pathEffect = DashPathEffect(floatArrayOf(10 * d, 8 * d), 0f) }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEFE8D8.toInt(); textAlign = Paint.Align.CENTER
        typeface = runCatching { ResourcesCompat.getFont(ctx, R.font.cormorant_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
    private val halo = Paint(text).apply { style = Paint.Style.STROKE; color = 0xCC10142A.toInt() }
    private val p = Point(); private val q = Point()
    private val centers = HashMap<String, GeoPoint>()

    private fun bmp(id: String, dp: Int): Bitmap? = bmps.getOrPut("$id@$dp") {
        val res = ctx.resources.getIdentifier(id, "drawable", ctx.packageName)
        if (res == 0) null else ResourcesCompat.getDrawable(ctx.resources, res, null)?.let { dr ->
            val w = (dp * d).toInt()
            val h = (w * dr.intrinsicHeight / dr.intrinsicWidth.toFloat()).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b -> dr.setBounds(0, 0, w, h); dr.draw(Canvas(b)) }
        }
    }

    private fun center(s: State, key: String): GeoPoint = centers.getOrPut(key) {
        val (la, lo) = Ley.center(s.homeLat, s.homeLon, Ley.Cell.of(key)); GeoPoint(la, lo)
    }

    /** Teal, or the aurora's greens and violets once it has been brewed. */
    private fun colour(s: State, key: String): Int {
        if (!s.aurora) return 0xFF45CFC6.toInt()
        val h = (app.leyline.domain.hash(key) and 0xff) / 255f
        val hsv = floatArrayOf(110f + h * 170f, 0.65f, 1f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    override fun draw(canvas: Canvas, map: MapView, shadow: Boolean) {
        if (shadow) return
        val s = state ?: return
        val pj = map.projection
        val zoom = map.zoomLevelDouble
        // quarters: a warm dotted border, ridden ones softly lit
        border.strokeWidth = 1.3f * d; border.pathEffect = DashPathEffect(floatArrayOf(2 * d, 5 * d), 0f)
        border.color = if (dark) 0x88F2C16B.toInt() else 0x99A0672A.toInt()
        for ((id, poly) in cells) {
            val qq = s.quarters[id] ?: continue
            if (poly.size < 3) continue
            val path = android.graphics.Path()
            poly.forEachIndexed { i, (la, lo) -> pj.toPixels(GeoPoint(la, lo), p); if (i == 0) path.moveTo(p.x.toFloat(), p.y.toFloat()) else path.lineTo(p.x.toFloat(), p.y.toFloat()) }
            path.close()
            if (qq.visits > 0) { fill.color = if (dark) 0x22F2C16B else 0x22C7832F; canvas.drawPath(path, fill) }
            canvas.drawPath(path, border)
        }
        // quarter names
        text.color = if (dark) 0xFFF7EDDC.toInt() else 0xFF3A2340.toInt(); halo.color = if (dark) 0xCC1E1524.toInt() else 0xCCF6EDE2.toInt()
        if (zoom >= 12.0) for (qq in s.quarters.values) {
            pj.toPixels(GeoPoint(qq.lat, qq.lon), p)
            text.textSize = 13 * d; halo.textSize = 13 * d; halo.strokeWidth = 3 * d
            text.alpha = if (qq.visits > 0) 200 else 110
            val ny = p.y + (if (qq.visits > 0 && qq.kind != null) 30 else 4) * d
            canvas.drawText(qq.name, p.x.toFloat(), ny, halo); canvas.drawText(qq.name, p.x.toFloat(), ny, text)
        }
        // the threads
        val scale = (zoom - 11).coerceIn(0.6, 4.0).toFloat()
        for ((k, n) in s.ley) {
            val (a, b) = k.split('|')
            pj.toPixels(center(s, a), p); pj.toPixels(center(s, b), q)
            val lv = Ley.level(n)
            val w = (1.2f + lv * 1.3f) * d * scale / 2
            val col = colour(s, k)
            glow.color = col; glow.alpha = 60 + lv * 40; glow.strokeWidth = w * 3
            canvas.drawLine(p.x.toFloat(), p.y.toFloat(), q.x.toFloat(), q.y.toFloat(), glow)
            core.color = if (lv >= 2) 0xFFE8FFFC.toInt() else col; core.alpha = 140 + lv * 35; core.strokeWidth = w
            canvas.drawLine(p.x.toFloat(), p.y.toFloat(), q.x.toFloat(), q.y.toFloat(), core)
        }
        dash.strokeWidth = 2.2f * d
        for ((k, _) in s.deep) {
            val (a, b) = k.split('|')
            pj.toPixels(center(s, a), p); pj.toPixels(center(s, b), q)
            canvas.drawLine(p.x.toFloat(), p.y.toFloat(), q.x.toFloat(), q.y.toFloat(), dash)
        }
        // the knots
        for (k in Ley.knots(s.ley)) {
            pj.toPixels(center(s, k), p)
            dot.color = 0x5545CFC6; canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 11 * d, dot)
            dot.color = 0xFFE8FFFC.toInt(); canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 3.5f * d, dot)
        }
        // the spirits sit in their quarters
        for (qq in s.quarters.values) {
            val k = qq.kind ?: continue
            if (qq.visits == 0) continue
            val met = k.id in s.spirits
            pj.toPixels(GeoPoint(qq.lat, qq.lon), p)
            val b = bmp("spirit_${k.id}", 40) ?: continue
            dot.color = if ((s.spirits[k.id] ?: 0) >= 4) 0x99E0B65A.toInt() else 0x8810142A.toInt()
            canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 24 * d, dot)
            val pa = Paint().apply { alpha = if (met) 255 else 110 }
            canvas.drawBitmap(b, p.x - b.width / 2f, p.y - b.height / 2f, pa)
        }
        // this week's blooming quarter: a ring of petals and its ingredient
        app.leyline.domain.Luck.bloom(s.quarters.values.filter { it.visits > 0 }.map { it.id })?.let { (qid, ing) ->
            val qq = s.quarters[qid] ?: return@let
            pj.toPixels(GeoPoint(qq.lat, qq.lon), p)
            val cy = p.y - 44 * d
            for (i in 0 until 8) {
                val a = i * Math.PI / 4
                dot.color = if (i % 2 == 0) 0xCCE7A0AE.toInt() else 0xCCF2C16B.toInt()
                canvas.drawCircle(p.x + (Math.cos(a) * 15 * d).toFloat(), cy + (Math.sin(a) * 15 * d).toFloat(), 6 * d, dot)
            }
            dot.color = 0xEE2A1F33.toInt(); canvas.drawCircle(p.x.toFloat(), cy, 14 * d, dot)
            bmp("ing_$ing", 24)?.let { b -> canvas.drawBitmap(b, p.x - b.width / 2f, cy - b.height / 2f, null) }
        }
        // the sanctums
        for (sc in s.sanctums) {
            if (!sc.awake) continue
            pj.toPixels(GeoPoint(sc.lat, sc.lon), p)
            val b = bmp(if (sc.ward.isNotEmpty()) app.leyline.domain.Book.recipe(sc.ward).pic else "ic_shrine", if (sc.ward.isNotEmpty()) 44 else 32) ?: continue
            dot.color = 0x4445CFC6; canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 22 * d, dot)
            canvas.drawBitmap(b, p.x - b.width / 2f, p.y - b.height + 8 * d, null)
        }
        fix?.let { (la, lo) ->
            pj.toPixels(GeoPoint(la, lo), p)
            dot.color = 0x5545CFC6; canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 16 * d, dot)
            dot.color = 0xFFEFE8D8.toInt(); canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 8 * d, dot)
            dot.color = 0xFF1E7F80.toInt(); canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 5.5f * d, dot)
            bmp(beastPic(s, s.beast), 46)?.let { b -> canvas.drawBitmap(b, p.x - b.width / 2f, p.y - b.height - 6 * d, null) }
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, map: MapView): Boolean {
        val s = state ?: return false
        val pj = map.projection
        val hit = s.sanctums.filter { it.awake }.minByOrNull { sc -> pj.toPixels(GeoPoint(sc.lat, sc.lon), p); Math.hypot((p.x - e.x).toDouble(), (p.y - e.y).toDouble()) }
        if (hit != null) {
            pj.toPixels(GeoPoint(hit.lat, hit.lon), p)
            if (Math.hypot((p.x - e.x).toDouble(), (p.y - e.y).toDouble()) < 34 * d) { onSanctum(hit.id); return true }
        }
        val g = pj.fromPixels(e.x.toInt(), e.y.toInt())
        val qq = Quarters.at(s.quarters.values, g.latitude, g.longitude) ?: return false
        onQuarter(qq.id)
        return true
    }

    override fun onLongPress(e: MotionEvent, map: MapView): Boolean {
        val f = onLong ?: return false
        val g = map.projection.fromPixels(e.x.toInt(), e.y.toInt())
        f(g.latitude, g.longitude)
        return true
    }
}

/** Night tiles: inverted, hue turned back so water stays blue, dimmed to indigo. */
fun nightTiles(): android.graphics.ColorMatrixColorFilter {
    val invert = android.graphics.ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
    val hue = android.graphics.ColorMatrix(floatArrayOf(-0.574f, 1.43f, 0.144f, 0f, 0f, 0.426f, 0.43f, 0.144f, 0f, 0f, 0.426f, 1.43f, -0.856f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
    invert.postConcat(hue)
    val dim = android.graphics.ColorMatrix(); dim.setScale(0.62f, 0.66f, 0.85f, 1f)
    invert.postConcat(dim)
    return android.graphics.ColorMatrixColorFilter(invert)
}

/** Day theme tiles: a dusky violet-blue wash, light enough to read the streets, dark enough for the glow. */
fun duskTiles(): android.graphics.ColorMatrixColorFilter {
    val m = android.graphics.ColorMatrix(); m.setSaturation(0.15f)
    val dusk = android.graphics.ColorMatrix(floatArrayOf(.42f, 0f, 0f, 0f, 30f, 0f, .44f, 0f, 0f, 34f, 0f, 0f, .5f, 0f, 70f, 0f, 0f, 0f, 1f, 0f))
    m.postConcat(dusk)
    return android.graphics.ColorMatrixColorFilter(m)
}

/** Dark theme tiles: the city by candlelight – inverted to night, then washed plum and amber. */
fun candleTiles(): android.graphics.ColorMatrixColorFilter {
    val m = android.graphics.ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
    val grey = android.graphics.ColorMatrix(); grey.setSaturation(0f)
    m.postConcat(grey)
    m.postConcat(android.graphics.ColorMatrix(floatArrayOf(.36f, 0f, 0f, 0f, 30f, 0f, .30f, 0f, 0f, 20f, 0f, 0f, .34f, 0f, 34f, 0f, 0f, 0f, 1f, 0f)))
    return android.graphics.ColorMatrixColorFilter(m)
}

/** Light theme tiles: an old sepia map with a rosy tint. */
fun sepiaTiles(): android.graphics.ColorMatrixColorFilter {
    val m = android.graphics.ColorMatrix(); m.setSaturation(0f)
    m.postConcat(android.graphics.ColorMatrix(floatArrayOf(.55f, 0f, 0f, 0f, 105f, 0f, .5f, 0f, 0f, 92f, 0f, 0f, .46f, 0f, 84f, 0f, 0f, 0f, 1f, 0f)))
    return android.graphics.ColorMatrixColorFilter(m)
}
