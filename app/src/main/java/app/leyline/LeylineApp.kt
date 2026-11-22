package app.leyline

import android.app.Application
import android.content.Context
import app.leyline.data.Game
import app.leyline.domain.Lang
import app.leyline.trip.Feed
import app.leyline.trip.TripService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Container(context: Context) {
    val app: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val game = Game(app, scope)
    val feed = Feed(app) { f -> scope.launch { game.onFix(f) } }

    /** Listens for a little while, so the current quarter is known before any trip. */
    fun locateOnce(seconds: Long = 20) {
        if (!feed.permitted() || BuildConfig.DEMO) return
        scope.launch(Dispatchers.Main) {
            feed.acquire()
            delay(seconds * 1000)
            feed.release()
        }
    }
}

class LeylineApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        // Tiles come from openstreetmap.org, whose policy asks for an app-specific user agent.
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = "Leyline/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
            osmdroidBasePath = java.io.File(cacheDir, "osmdroid")
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid/tiles")
            tileFileSystemCacheMaxBytes = 200L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 160L * 1024 * 1024
        }
        container = Container(this)
        container.game.load()
        TripService.channels(this)
        app.leyline.trip.Auto.channels(this)
    }
}

val Context.container: Container get() = (applicationContext as LeylineApp).container
