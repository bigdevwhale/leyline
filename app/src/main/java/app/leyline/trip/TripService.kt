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
 */
class TripService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var holding = false
    private var ticker: Job? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val c = container
        if (intent?.action == ACTION_STOP) {
            scope.launch { c.game.endTrip(); stopSelf() }
            return START_NOT_STICKY
        }
        if (!c.feed.permitted() || c.game.now.trip == null) { stopSelf(); return START_NOT_STICKY }
        try {
            ServiceCompat.startForeground(this, ID, note(this), if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (_: Exception) { stopSelf(); return START_NOT_STICKY }
        if (!holding) {
            holding = true
            c.feed.acquire()
            ticker = scope.launch {
                while (true) {
                    delay(15_000)
                    if (c.game.now.trip == null) { stopSelf(); break }
                    if (c.game.idle()) { c.game.endTrip(); stopSelf(); break }
                    getSystemService(NotificationManager::class.java).notify(ID, note(this@TripService))
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
        if (holding) container.feed.release()
        holding = false
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

        fun note(c: Context): Notification {
            val g = c.container.game
            val t = g.now.trip
            val q = g.now.quarters[g.now.here]
            val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
            val stop = PendingIntent.getService(c, 1, Intent(c, TripService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
            val text = if (t == null) "" else tr("${fmt1(t.km)} км · ${Ride.of(t.ride).beast.ru} собирает силу дороги", "${fmt1(t.km)} km · your ${Ride.of(t.ride).beast.en.lowercase()} gathers the road's power")
            return NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(q?.let { tr("В пути · ${it.ru}", "On the way · ${it.en}") } ?: tr("В пути", "On the way"))
                .setContentText(text)
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
                .setContentIntent(open)
                .addAction(0, tr("Приехали", "Arrived"), stop)
                .build()
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

        fun stop(c: Context) {
            val k = c.container
            k.scope.launch { k.game.endTrip() }
            c.stopService(Intent(c, TripService::class.java))
        }
    }
}
