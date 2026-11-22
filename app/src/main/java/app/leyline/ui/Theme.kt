package app.leyline.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.leyline.R

/**
 * Candlelight: panels of warm plum glass over the pictures, edged with honey light that fades into rose gold.
 * Teal is kept for magic itself – the ley lines and runes. The light theme is the same room by day: rose cream and plum ink.
 * ([paper] and [paper2] are the glass and the deeper glass; [glow] is honey, [violet] rose gold.)
 */
class Pal(
    val dark: Boolean, val bg: Color, val paper: Color, val paper2: Color, val ink: Color, val mute: Color, val line: Color,
    val acc: Color, val accInk: Color, val gold: Color, val goldSoft: Color, val indigo: Color, val teal: Color, val ok: Color, val bad: Color, val shade: Color,
    val violet: Color, val glow: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xF4ECE4), c(0xFFF9F2), c(0xF1E3D8), c(0x3A2340), c(0x86707F), c(0xE4D0C4),
    c(0xC7832F), c(0xFFF8EE), c(0xC7832F), c(0xF6E2BE), c(0x5A3D78), c(0x17A396), c(0x3E8A5A), c(0xC0452E), Color(0x33201028),
    c(0xD0687E), c(0xE0A04A))

val Dark = Pal(true, c(0x150F1C), c(0x2A1F33), c(0x372A41), c(0xF7EDDC), c(0xC3B1C0), c(0x4D3D58),
    c(0xF2C16B), c(0x2A1606), c(0xF2C16B), c(0x3E3122), c(0x4A3466), c(0x5FD9CB), c(0x8DCB8F), c(0xFF8A6E), Color(0x66000000),
    c(0xE7A0AE), c(0xF2C16B))

object C {
    @Volatile var p: Pal = Dark
    val dark get() = p.dark
    val bg get() = p.bg; val paper get() = p.paper; val paper2 get() = p.paper2; val ink get() = p.ink; val mute get() = p.mute; val line get() = p.line
    val acc get() = p.acc; val accInk get() = p.accInk; val gold get() = p.gold; val goldSoft get() = p.goldSoft; val indigo get() = p.indigo; val teal get() = p.teal
    val ok get() = p.ok; val bad get() = p.bad; val shade get() = p.shade; val violet get() = p.violet; val glow get() = p.glow
    /** The thread of light: teal into violet. */
    val thread get() = Brush.horizontalGradient(listOf(p.glow, p.violet))
}

/** Cormorant for titles – a spell-book serif; Alegreya Sans for reading. */
val Serif = FontFamily(Font(R.font.cormorant_semibold, FontWeight.SemiBold), Font(R.font.cormorant_bold, FontWeight.Bold))
val Sans = FontFamily(Font(R.font.alegreya_regular, FontWeight.Normal), Font(R.font.alegreya_medium, FontWeight.Medium), Font(R.font.alegreya_bold, FontWeight.Bold))
/** The old names, so every screen picks the new letters up. */
val Phil get() = Serif

object T {
    val big get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp, color = C.ink)
    val title get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 30.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 24.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 18.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 15.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 26.sp, color = C.ink)
    val huge get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 62.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp)
}

@Composable
fun LeylineTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.paper, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.paper, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
