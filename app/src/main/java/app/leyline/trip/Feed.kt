package app.leyline.trip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One GPS subscription shared by the screen (to know the quarter) and the trip service. Plain
 * LocationManager, so it works on phones without Google services.
 */
class Feed(private val context: Context, private val deliver: (Fix) -> Unit) {
    private val lm = context.getSystemService(LocationManager::class.java)
    private var users = 0
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = take(location)
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    }

    fun permitted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Main thread only. */
    fun acquire() {
        users++
        if (users == 1) start()
    }

    fun release() {
        if (users == 0) return
        users--
        if (users == 0) lm.removeUpdates(listener)
    }

    fun restart() {
        if (users == 0) return
        lm.removeUpdates(listener)
        start()
    }

    /** The freshest position the phone already has, if any (under 15 minutes old). */
    fun last(): Fix? = try {
        if (!permitted()) null else lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }
            .filter { System.currentTimeMillis() - it.time < 6 * 3600_000L }.minByOrNull { it.accuracy }?.let(::fix)
    } catch (_: SecurityException) { null }

    /**
     * One position, without keeping the GPS on: listens for at most [ms] and gives the best fix it heard (or a
     * fresh one the phone already had), stopping early at one within [good] metres. Null when nothing came –
     * deep under the ground, say.
     */
    suspend fun once(ms: Long, good: Float = 30f): Fix? {
        if (app.leyline.BuildConfig.DEMO || !permitted()) return null
        var best: Fix? = last()?.takeIf { System.currentTimeMillis() - it.time < 60_000 }
        if (best != null && best.accuracy <= good) return best
        val heard = Channel<Fix>(Channel.UNLIMITED)
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) { heard.trySend(fix(location)) }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            withContext(Dispatchers.Main) {
                val providers = lm.getProviders(true)
                for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER))
                    if (p in providers) lm.requestLocationUpdates(p, 1000L, 0f, l, Looper.getMainLooper())
            }
            withTimeoutOrNull(ms) {
                for (f in heard) {
                    val b = best
                    if (b == null || f.accuracy < b.accuracy) best = f
                    if (f.accuracy <= good) break
                }
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { lm.removeUpdates(l) }
        }
        return best
    }

    private fun start() {
        if (app.leyline.BuildConfig.DEMO || !permitted()) return
        try {
            val providers = lm.getProviders(false)
            if (LocationManager.GPS_PROVIDER in providers) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper())
            if (LocationManager.NETWORK_PROVIDER in providers) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10_000L, 0f, listener, Looper.getMainLooper())
            last()?.let(deliver)
            // a fresh one-off position right away, so the dot shows without waiting for the satellites
            if (android.os.Build.VERSION.SDK_INT >= 30) for (p in listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)) {
                if (p in providers && lm.isProviderEnabled(p)) lm.getCurrentLocation(p, null, ContextCompat.getMainExecutor(context)) { l -> if (l != null) take(l) }
            }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun fix(l: Location) = Fix(l.latitude, l.longitude, if (l.hasAccuracy()) l.accuracy else 80f, l.time,
        if (l.hasSpeed()) l.speed else null, l.provider == LocationManager.GPS_PROVIDER)

    private fun take(l: Location) = deliver(fix(l))
}
