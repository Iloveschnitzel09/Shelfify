package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.funktionen.sub.DatagroupService
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.inviteGroup
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.joinGroup
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.leaveGroup
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SettingsActivity : AppCompatActivity() {
    private lateinit var editTextEmail: EditText
    private lateinit var editTextVerificationCode: EditText
    private lateinit var tvVerificationStatus: TextView
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchNotifications: Switch
    private lateinit var btnVerify: Button
    private lateinit var btnRequestCode: Button
    private lateinit var btnInvite: Button
    private lateinit var etJoinCode: EditText
    private lateinit var btnJoin: Button
    private lateinit var btnLeave: Button
    private lateinit var etInviteEmail: EditText

    private val baseUrl: String = ApiConfig.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // UI-Elemente initialisieren
        editTextEmail = findViewById(R.id.etEmail)
        editTextVerificationCode = findViewById(R.id.etVerificationCode)
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus)
        switchNotifications = findViewById(R.id.switchNotifications)
        btnRequestCode = findViewById(R.id.btnRequestCode)
        btnVerify = findViewById(R.id.btnVerify)
        btnInvite = findViewById(R.id.btnInvite)
        etInviteEmail = findViewById(R.id.etInviteEmail)
        etJoinCode = findViewById(R.id.etJoinCode)
        btnJoin = findViewById(R.id.btnJoin)
        btnLeave = findViewById(R.id.btnLeave)

        val btnSaveEmail: Button = findViewById(R.id.btnSaveEmail)

        val email = prefs.getString("email", "null") ?: "null"
        val token = prefs.getString("token", "null") ?: "null"

        // Aktuelle E-Mail-Adresse und Status laden
        loadCurrentSettings(prefs)

        btnSaveEmail.setOnClickListener {
            val etemail = editTextEmail.text.toString().trim()
            if (isValidEmail(etemail)) {
                saveEmail(etemail, prefs)
            } else {
                Toast.makeText(this, "Bitte gültige E-Mail-Adresse eingeben", Toast.LENGTH_SHORT).show()
            }
        }

        btnRequestCode.setOnClickListener {
            if (email != "null" || token != "null") {
                requestVerificationCode(email, token, prefs)
            } else {
                Toast.makeText(this, "Bitte gültige E-Mail-Adresse eingeben", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerify.setOnClickListener {
            val code = editTextVerificationCode.text.toString().trim()
            if (code.length == 6) {
                verifyCode(email, code, token, prefs)
            } else {
                Toast.makeText(this, "Bitte gültigen 6-stelligen Code eingeben", Toast.LENGTH_SHORT).show()
            }
        }

        switchNotifications.setOnClickListener { buttonView ->
            if (prefs.getBoolean("verify", false)) {
                setNotificationPreference(email, true, token, prefs)
                prefs.edit { putBoolean("verify", true) }
            } else {
                switchNotifications.isChecked = false
                setNotificationPreference(email, false, token, prefs)
                prefs.edit { putBoolean("verify", false) }
                Toast.makeText(this, "E-Mail muss erst verifiziert werden", Toast.LENGTH_SHORT).show()
            }
        }

        btnInvite.setOnClickListener {
            disableButton(btnInvite, " Warte ", " Einladen ", 30)

            val email = etInviteEmail.text.toString().trim()
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Bitte gültige E-Mail-Adresse eingeben", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            inviteGroup(prefs, email,this)
        }

        btnJoin.setOnClickListener {
            val code = etJoinCode.text.toString()
            if (code.isEmpty() || code.length != 6) {
                Toast.makeText(this, "Bitte gültigen Einladungscode eingeben", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            joinGroup(prefs, code, this)
        }

        btnLeave.setOnClickListener {
            leaveGroup(prefs, this)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadCurrentSettings(prefs: SharedPreferences) {
        Thread {
            try {
                // Email anzeigen
                val email = prefs.getString("email", "null") ?: "null"
                if (email != "null") {
                    runOnUiThread { editTextEmail.setText(email) }
                }
                // Verifizierungsstatus anzeigen
                val verified = prefs.getBoolean("verify", false)
                runOnUiThread {
                    if (verified) {
                        tvVerificationStatus.text = "Verifiziert"
                        tvVerificationStatus.setTextColor(Color.GREEN)
                        switchNotifications.isEnabled = true
                        btnVerify.isEnabled = false
                        btnRequestCode.isEnabled = false
                    } else {
                        tvVerificationStatus.text = "Nicht verifiziert"
                        tvVerificationStatus.setTextColor(Color.RED)
                        switchNotifications.isEnabled = false
                        btnVerify.isEnabled = true
                        btnRequestCode.isEnabled = true
                    }
                }

                // Notify Status anzeigen
                val notify = prefs.getBoolean("notify", false)
                runOnUiThread { switchNotifications.isChecked = notify }

            } catch (e: Exception) {
                Log.e("loadSett", "Error: $e")
            }
        }.start()
    }

    private fun requestVerificationCode(email: String, token: String, prefs: SharedPreferences) {
        Thread {
            try {
                disableButton(btnRequestCode, "Erneut senden in", "Code erneut anfordern", 60 )

                val url = URL("$baseUrl/requestVerificode")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "email=${URLEncoder.encode(email, "UTF-8")}&token=$token"
                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(StandardCharsets.UTF_8))
                }
                val responseCode = conn.responseCode

                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Verifizierungscode wurde gesendet", Toast.LENGTH_SHORT).show()
                        loadCurrentSettings(prefs)
                    } else {
                        Toast.makeText(this, "Fehler beim Senden des Codes$responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("rqv", "Error: $e")
                runOnUiThread { Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun verifyCode(email: String, code: String, token: String, prefs: SharedPreferences) {
        Thread {
            try {
                val url = URL("$baseUrl/verifyCode")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "email=${URLEncoder.encode(email, "UTF-8")}&code=${URLEncoder.encode(code, "UTF-8")}&token=$token"
                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(StandardCharsets.UTF_8))
                }

                val responseCode = conn.responseCode
                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "E-Mail erfolgreich verifiziert", Toast.LENGTH_SHORT).show()
                        prefs.edit { putBoolean("verify", true) }
                        loadCurrentSettings(prefs)
                    } else {
                        Toast.makeText(this, "Ungültiger Code", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun setNotificationPreference(email: String, notify: Boolean, token: String, prefs: SharedPreferences) {
        Thread {
            try {
                val url = URL("$baseUrl/setNotifyPreference")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "email=${URLEncoder.encode(email, StandardCharsets.UTF_8)}&token=$token"
                } else {
                    "email=${URLEncoder.encode(email, "UTF-8")}&token=$token"
                }

                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(StandardCharsets.UTF_8))
                }

                val responseCode = conn.responseCode
                runOnUiThread {
                    if (responseCode == 200) {
                        prefs.edit { putBoolean("notify", notify) }
                        Toast.makeText(this, "Benachrichtigungseinstellungen gespeichert", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Fehler beim Speichern der Einstellungen", Toast.LENGTH_SHORT).show()
                        switchNotifications.isChecked = !notify
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveEmail(email: String, prefs: SharedPreferences) {
        Thread {
            try {
                val id = prefs.getInt("app_id", -1)
                val token = prefs.getString("token", null)
                Log.v("test", "$token $id")
                if (id == -1 || token == null) {
                    runOnUiThread { Toast.makeText(this, "App-ID oder Token fehlt", Toast.LENGTH_SHORT).show() }
                    return@Thread
                }

                val url = URL("$baseUrl/setEmail")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "email=${URLEncoder.encode(email, "UTF-8")}" +
                        "&id=$id" +
                        "&token=${URLEncoder.encode(token, "UTF-8")}"

                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(StandardCharsets.UTF_8))
                }

                val responseCode = conn.responseCode
                runOnUiThread {
                    when (responseCode) {
                        200 -> {
                            Toast.makeText(this, "E-Mail-Adresse gespeichert", Toast.LENGTH_SHORT).show()
                            prefs.edit().apply {
                                putString("email", email)
                                putBoolean("verify", false)
                                putBoolean("notify", false)
                                apply()
                            }
                            loadCurrentSettings(prefs)
                        }
                        409 -> Toast.makeText(this, "Diese E-Mail wird bereits verwendet", Toast.LENGTH_SHORT).show()
                        401 -> Toast.makeText(this, "Ungültiger Token", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                    }
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun disableButton(button: Button, tickText: String, finishText: String, seconds: Long) {
        runOnUiThread {
            button.isEnabled = false
            object : CountDownTimer(seconds * 1000, 1000) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    button.text = "$tickText ${millisUntilFinished / 1000}s"
                }


                override fun onFinish() {
                    button.isEnabled = true
                    button.text = finishText
                }
            }.start()
        }
    }
}