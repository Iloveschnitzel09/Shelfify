package de.schnitzel.shelfify.api

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import de.schnitzel.shelfify.funktionen.NoConnectionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * Prüft die Verbindung zum Shelfify-Server und sperrt die App über
 * den NoConnectionActivity-Screen, wenn der Server nicht erreichbar ist.
 * Entscheidend ist nur die Server-Antwort, nicht ob "Internet" da ist.
 *
 * Auslöser für eine Prüfung:
 *  - fehlgeschlagene API-Requests (über ApiClient)
 *  - Verlust eines Netzwerks
 *  - Activity kommt in den Vordergrund (mit Drossel)
 *  - regelmäßig, solange eine Activity im Vordergrund ist
 */
object ConnectionGuard {

    // Regelmäßige Prüfung, solange eine Activity im Vordergrund ist
    private const val CHECK_INTERVAL_MS = 30 * 1000L

    // Beim Resume nicht öfter als alle 30 s prüfen (Activity-Wechsel sind häufig)
    private const val RESUME_THROTTLE_MS = 30 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val syncMutex = Mutex()
    private lateinit var app: Application

    private var resumedActivity: WeakReference<Activity>? = null
    private var lockActivity: WeakReference<NoConnectionActivity>? = null
    private var lockRequestedAt = 0L
    private var checking = false
    private var periodicJob: Job? = null
    private var lastSuccessfulSync = 0L

    /**
     * Zählt, wie oft die App nach einem Verbindungsverlust wieder verbunden wurde.
     * Activities vergleichen den Wert in onResume und laden ihre Daten neu, wenn er sich geändert hat.
     */
    @Volatile
    var reconnectCount = 0
        private set

    fun init(application: Application) {
        app = application
        registerLifecycleCallbacks()
        registerNetworkCallback()
    }

    /**
     * Synchronisiert mit dem Server. Läuft immer nur einmal gleichzeitig,
     * damit nicht zwei parallele Syncs jeweils einen neuen Account anlegen.
     */
    suspend fun syncNow(): Boolean = syncMutex.withLock {
        val ok = syncWithServer(app.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE))
        if (ok) lastSuccessfulSync = SystemClock.elapsedRealtime()
        ok
    }

    /** Wird vom Sperrscreen benutzt. Bei Erfolg laden die Activities ihre Daten neu. */
    suspend fun retryFromLock(): Boolean {
        val ok = syncNow()
        if (ok) reconnectCount++
        return ok
    }

    /** Wird vom ApiClient bei jedem Verbindungsfehler aufgerufen (beliebiger Thread). */
    fun reportConnectionFailure() {
        mainHandler.post {
            if (resumedActivity?.get() != null) checkServer(respectThrottle = false)
        }
    }

    private fun registerLifecycleCallbacks() {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is NoConnectionActivity) lockActivity = WeakReference(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (activity is NoConnectionActivity && lockActivity?.get() === activity) {
                    lockActivity = null
                }
            }

            override fun onActivityResumed(activity: Activity) {
                resumedActivity = WeakReference(activity)
                if (activity is NoConnectionActivity) return

                checkServer(respectThrottle = true)
                startPeriodicCheck()
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumedActivity?.get() === activity) resumedActivity = null
                stopPeriodicCheck()
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    /**
     * Netzwerk-Events sind nur ein Auslöser für eine sofortige Server-Prüfung.
     * Ob gesperrt wird, entscheidet allein die Server-Antwort.
     */
    private fun registerNetworkCallback() {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                mainHandler.postDelayed({
                    if (resumedActivity?.get() != null) checkServer(respectThrottle = false)
                }, 1500)
            }
        })
    }

    private fun startPeriodicCheck() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (true) {
                delay(CHECK_INTERVAL_MS.milliseconds)
                checkServer(respectThrottle = false)
            }
        }
    }

    private fun stopPeriodicCheck() {
        periodicJob?.cancel()
        periodicJob = null
    }

    private fun checkServer(respectThrottle: Boolean) {
        // Solange der Sperrscreen offen ist, versucht er sich selbst erneut
        if (checking || isLockShowing()) return
        if (respectThrottle &&
            SystemClock.elapsedRealtime() - lastSuccessfulSync < RESUME_THROTTLE_MS
        ) return

        checking = true
        scope.launch {
            try {
                if (!syncNow()) showLock()
            } finally {
                checking = false
            }
        }
    }

    /** true, wenn die Sperr-Activity wirklich existiert oder gerade gestartet wird. */
    private fun isLockShowing(): Boolean {
        val lock = lockActivity?.get()
        if (lock != null && !lock.isFinishing && !lock.isDestroyed) return true

        // Start wurde angefordert, die Activity ist aber noch nicht erstellt
        return SystemClock.elapsedRealtime() - lockRequestedAt < 2000
    }

    private fun showLock() {
        // Ist die App im Hintergrund, wird beim nächsten Resume neu geprüft
        val activity = resumedActivity?.get() ?: return
        if (activity is NoConnectionActivity || isLockShowing()) return

        lockRequestedAt = SystemClock.elapsedRealtime()
        activity.startActivity(Intent(activity, NoConnectionActivity::class.java))
    }
}