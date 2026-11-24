package app.leyline.trip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.leyline.MainActivity
import app.leyline.R
import app.leyline.container
import app.leyline.domain.Metro
import app.leyline.domain.Ride
import app.leyline.domain.fmt1
import app.leyline.domain.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * A ride with the screen off: GPS on in a foreground service, a quiet notification with the way so far, an
 * optional voice for new quarters. It ends itself after eight still minutes.
 *
 * By metro the GPS stays off: there are no satellites under the ground, only a flat battery. One position is
 * taken at the entrance and one at «Arrived»; when either can't be found, the station is picked by hand.
 */
class TripService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var holding = false
    /** The GPS is kept on (not by metro). */
    private var listening = false
    /** Looking for the metro's exit. */
    private var seeking = false
    private var lost = false
    private var ticker: Job? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val c = container
        if (intent?.action == ACTION_STOP) {
            val t = c.game.now.trip
            if (t == null || Ride.of(t.ride) != Ride.METRO || !holding) {
                scope.launch { c.game.endTrip(); stopSelf() }
                return START_NOT_STICKY
            }
            if (!seeking) exit()
            return START_STICKY
        }
        if (!c.feed.permitted() || c.game.now.trip == null) { stopSelf(); return START_NOT_STICKY }
        try {
            ServiceCompat.startForeground(this, ID, note(this), if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (_: Exception) { stopSelf(); return START_NOT_STICKY }
        if (!holding) {
            holding = true
            if (Ride.of(c.game.now.trip?.ride ?: "") == Ride.METRO) entrance() else { listening = true; c.feed.acquire() }
            ticker = scope.launch {
                while (true) {
                    delay(15_000)
                    if (c.game.now.trip == null) { stopSelf(); break }
                    if (c.game.idle()) { c.game.endTrip(); stopSelf(); break }
                    if (!seeking) refresh()
                }
            }
            scope.launch {
                c.game.news.collect { n ->
                    if (n.speak && c.game.now.voice) say(n.text)
                }
            }
        }
        return START_STICKY
    }

    private fun refresh() = getSystemService(NotificationManager::class.java).notify(ID, note(this, lost))

    /** By metro: the stations are loaded while the network is still there, and the entrance is found once. */
    private fun entrance() {
        val c = container
        scope.launch { c.game.stations() }
        scope.launch {
            if (c.game.now.trip?.lastAt != 0L) return@launch
            val t = System.currentTimeMillis()
            val f = c.game.live.value.fix?.takeIf { t - it.time < 90_000 && it.accuracy <= Metro.GOOD }
                ?: c.feed.once(30_000)?.takeIf { it.accuracy <= Metro.GOOD }
            if (f != null) c.game.metroFrom(f.lat, f.lon, auto = true)
            refresh()
        }
    }

    /** «Arrived» by metro from the notification: one position at the exit. Not found, the app asks for the station. */
    private fun exit() {
        val c = container
        seeking = true; lost = false
        getSystemService(NotificationManager::class.java).notify(ID, seekNote(this))
        scope.launch {
            // with no entrance the exit alone makes no ride: the app asks for both
            val f = if (c.game.now.trip?.lastAt == 0L) null else c.feed.once(30_000)?.takeIf { it.accuracy <= Metro.GOOD }
            seeking = false
            if (f != null && c.game.now.trip != null) { c.game.metroEnd(f.lat, f.lon); stopSelf() }
            else { lost = true; refresh() }
        }
    }

    private fun say(text: String) {
        val t = tts
        if (t == null) {
            tts = TextToSpeech(this) { ok ->
                ttsReady = ok == TextToSpeech.SUCCESS
                if (ttsReady) { tts?.language = if (app.leyline.domain.Lang.ru) Locale.forLanguageTag("ru") else Locale.UK; tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "c") }
            }
        } else if (ttsReady) t.speak(text, TextToSpeech.QUEUE_ADD, null, "c")
    }

    override fun onDestroy() {
        if (listening) container.feed.release()
        holding = false; listening = false
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "app.leyline.STOP_TRIP"
        const val ID = 7
        const val CHANNEL = "trip"

        fun channels(c: Context) {
            if (Build.VERSION.SDK_INT >= 26) c.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, tr("Поездка", "Trip"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        }

        /** [lost]: by metro, the exit wasn't found and the station has to be picked in the app. */
        fun note(c: Context, lost: Boolean = false): Notification {
            val g = c.container.game
            val t = g.now.trip
            val q = g.now.quarters[g.now.here]
            val metro = t != null && Ride.of(t.ride) == Ride.METRO
            val text = when {
                t == null -> ""
                lost -> tr("Не нашёл, где вы вышли. Откройте и выберите станцию", "Couldn't find where you came out. Open and pick the station")
                metro && t.lastAt == 0L -> tr("Не нашёл вход. Откройте и выберите станцию", "Couldn't find the entrance. Open and pick the station")
                metro -> tr("GPS спит до выхода. На выходе – «Приехали»", "The GPS sleeps till the exit. Out there – «Arrived»")
                else -> tr("${fmt1(t.km)} км · ${Ride.of(t.ride).beast.ru} собирает силу дороги", "${fmt1(t.km)} km · your ${Ride.of(t.ride).beast.en.lowercase()} gathers the road's power")
            }
            val title = if (metro) g.metroStart()?.let { tr("Метро · от «${it.ru}»", "Metro · from ${it.en}") } ?: tr("Метро", "Metro")
                else q?.let { tr("В пути · ${it.ru}", "On the way · ${it.en}") } ?: tr("В пути", "On the way")
            return base(c).setContentTitle(title).setContentText(text).build()
        }

        private fun seekNote(c: Context): Notification =
            base(c).setContentTitle(tr("Метро", "Metro")).setContentText(tr("Ищу, где вы вышли…", "Finding where you came out…")).build()

        private fun base(c: Context): NotificationCompat.Builder {
            val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
            val stop = PendingIntent.getService(c, 1, Intent(c, TripService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
            return NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setContentIntent(open)
                .addAction(0, tr("Приехали", "Arrived"), stop)
        }

        fun start(c: Context, ride: Ride) {
            val k = c.container
            if (!k.feed.permitted()) return
            k.scope.launch {
                k.game.startTrip(ride)
                ContextCompat.startForegroundService(c, Intent(c, TripService::class.java))
            }
        }

        /** Resumes a trip left on by a killed process. */
        fun resume(c: Context) {
            if (c.container.game.now.trip != null && c.container.feed.permitted()) ContextCompat.startForegroundService(c, Intent(c, TripService::class.java))
        }

        /** The metro's exit, picked in the app: the ride ends there. */
        fun arrive(c: Context, lat: Double, lon: Double) {
            val k = c.container
            k.scope.launch { k.game.metroEnd(lat, lon); c.stopService(Intent(c, TripService::class.java)) }
        }

        fun stop(c: Context) {
            val k = c.container
            // by metro the exit is looked for first, by the running service (it may still use location)
            if (k.game.now.trip?.let { Ride.of(it.ride) == Ride.METRO } == true &&
                runCatching { c.startService(Intent(c, TripService::class.java).setAction(ACTION_STOP)) }.isSuccess) return
            k.scope.launch { k.game.endTrip() }
            c.stopService(Intent(c, TripService::class.java))
        }
    }
}
