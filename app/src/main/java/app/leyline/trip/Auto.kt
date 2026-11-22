package app.leyline.trip

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.leyline.MainActivity
import app.leyline.R
import app.leyline.container
import app.leyline.domain.Ride
import app.leyline.domain.tr
import kotlinx.coroutines.launch

/**
 * So that a ride isn't forgotten. Android lets an app use location in a service it started only from the
 * foreground (or with «allow all the time», which Leyline doesn't ask for), so the car's Bluetooth can't start
 * the ride silently: it rings instead – one tap on the notification and the ride is on. When the car's
 * Bluetooth goes, the ride is over at once.
 */
object Auto {
    const val CHANNEL = "ready"
    const val ID = 8
    const val EXTRA_GO = "go"

    fun channels(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) c.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Пора в путь", "Time to set off"), NotificationManager.IMPORTANCE_HIGH).apply { setShowBadge(false) })
    }

    fun permitted(c: Context) = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /** Paired devices as address to name, for choosing the car. */
    @SuppressLint("MissingPermission")
    fun paired(c: Context): List<Pair<String, String>> = runCatching {
        if (!permitted(c)) emptyList() else c.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices.orEmpty()
            .map { it.address to (it.name ?: it.address) }.sortedBy { it.second.lowercase() }
    }.getOrDefault(emptyList())

    fun goIntent(c: Context, ride: Ride): PendingIntent = PendingIntent.getActivity(c, 20 + ride.ordinal,
        Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(EXTRA_GO, ride.id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** «Your beast is ready»: tap to set off. Goes away by itself after ten minutes. */
    fun ring(c: Context, name: String) {
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(tr("Дрейк готов в путь", "Your drake is ready"))
            .setContentText(tr("$name на связи. Нажмите – и дорога начнёт варить зелья.", "$name is connected. Tap and the road starts brewing."))
            .setContentIntent(goIntent(c, Ride.CAR)).setAutoCancel(true).setTimeoutAfter(10 * 60_000L)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching { c.getSystemService(NotificationManager::class.java).notify(ID, n) }
    }

    fun hush(c: Context) = runCatching { c.getSystemService(NotificationManager::class.java).cancel(ID) }
}

/** The car's Bluetooth came or went. */
class BtReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val g = c.container.game
        val want = g.now.btAddr
        if (want.isEmpty() || !g.now.onboarded) return
        val dev: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        if (dev?.address != want) return
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> if (g.now.trip == null) Auto.ring(c, g.now.btName.ifEmpty { tr("Машина", "The car") })
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Auto.hush(c)
                // the engine is off: a ride by car is over, with no eight-minute wait
                if (g.now.trip?.let { Ride.of(it.ride).driver } == true) {
                    val pending = goAsync()
                    c.container.scope.launch {
                        runCatching { g.endTrip() }
                        runCatching { c.stopService(Intent(c, TripService::class.java)) }
                        pending.finish()
                    }
                }
            }
        }
    }
}

/** A quick-settings tile: «Set off» with the last way of riding, or «Arrived» while on the way. */
class TripTile : TileService() {
    override fun onStartListening() = refresh()

    private fun refresh() {
        val t = qsTile ?: return
        val on = container.game.now.trip != null
        t.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = if (on) tr("Приехали", "Arrived") else tr("В путь", "Set off")
        t.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val g = container.game
        if (g.now.trip != null) { TripService.stop(this); qsTile?.let { it.state = Tile.STATE_INACTIVE; it.label = tr("В путь", "Set off"); it.updateTile() }; return }
        val ride = Ride.of(g.now.ride)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(Auto.goIntent(this, ride))
        else @Suppress("DEPRECATION") startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(Auto.EXTRA_GO, ride.id))
    }
}
