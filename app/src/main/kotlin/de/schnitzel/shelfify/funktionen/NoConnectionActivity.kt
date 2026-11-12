package de.schnitzel.shelfify.funktionen

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.schnitzel.shelfify.R
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import de.schnitzel.shelfify.api.ConnectionGuard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class NoConnectionActivity : AppCompatActivity() {

    private lateinit var btnRetry: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvStatus: TextView

    private var busy = false
    private var autoRetryJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_connection)

        btnRetry = findViewById(R.id.btnRetry)
        progress = findViewById(R.id.progress)
        tvStatus = findViewById(R.id.tvStatus)

        btnRetry.setOnClickListener {
            lifecycleScope.launch { attempt(manual = true) }
        }

        // Zurück-Taste = App komplett beenden, nicht zurück in die gesperrte App
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        // Solange der Screen sichtbar ist, automatisch erneut versuchen
        autoRetryJob = lifecycleScope.launch {
            while (true) {
                delay(AUTO_RETRY_MS.milliseconds)
                attempt(manual = false)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        autoRetryJob?.cancel()
        autoRetryJob = null
    }

    private suspend fun attempt(manual: Boolean) {
        if (busy) return
        busy = true

        if (manual) {
            btnRetry.isEnabled = false
            progress.visibility = View.VISIBLE
            tvStatus.text = "Verbinde mit dem Server …"
        }

        val ok = ConnectionGuard.retryFromLock()

        busy = false
        if (manual) {
            progress.visibility = View.GONE
            btnRetry.isEnabled = true
        }

        if (ok) {
            finish() // zurück in die Activity, die vorher offen war (lädt ihre Daten neu)
        } else if (manual) {
            tvStatus.text = "Der Server ist nicht erreichbar.\nBitte Verbindung prüfen und erneut versuchen."
        }
    }

    private companion object {
        const val AUTO_RETRY_MS = 5000L
    }
}