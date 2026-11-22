package app.leyline.trip

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

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
