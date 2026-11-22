package app.leyline.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** The weather at the end of a ride, from Open-Meteo (no key). */
object Weather {
    /** [sky]: "rain", "snow", "fog", "clear", "cloud" or "" without the network; [temp] in °C. */
    data class Now(val sky: String, val temp: Double?)

    suspend fun now(lat: Double, lon: Double): Now = withTimeoutOrNull(6000) {
        withContext(Dispatchers.IO) {
            runCatching {
                val url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=weather_code,temperature_2m", lat, lon)
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 4000; c.readTimeout = 4000
                c.setRequestProperty("User-Agent", "Leyline (app.leyline)")
                val cur = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
                val code = cur.getInt("weather_code")
                val sky = when (code) {
                    0, 1 -> "clear"
                    45, 48 -> "fog"
                    in 51..67, in 80..82, in 95..99 -> "rain"
                    in 71..77, 85, 86 -> "snow"
                    else -> "cloud"
                }
                Now(sky, cur.optDouble("temperature_2m").takeIf { !it.isNaN() })
            }.getOrDefault(Now("", null))
        }
    } ?: Now("", null)

    fun title(w: String) = when (w) {
        "rain" -> tr("дождь", "rain"); "snow" -> tr("снег", "snow"); "fog" -> tr("туман", "fog")
        "clear" -> tr("ясно", "clear"); "cloud" -> tr("облачно", "cloudy"); else -> ""
    }
}
