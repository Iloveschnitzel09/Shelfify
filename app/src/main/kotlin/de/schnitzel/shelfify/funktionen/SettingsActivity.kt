package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.inviteGroup
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.joinGroup
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.leaveGroup
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.disableButton
import de.schnitzel.shelfify.util.syncWithServer
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.util.adapter.MemberAdapter
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class SettingsActivity : AppCompatActivity() {

    val client = OkHttpClient()
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
    private lateinit var  btnDeleteData : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


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
        btnDeleteData = findViewById(R.id.btnDelete)

        val btnSaveEmail: Button = findViewById(R.id.btnSaveEmail)

        val verifyHeader : TextView = findViewById(R.id.tvVerificationHeader)
        val verifySection : LinearLayout = findViewById(R.id.verificationSection)
        val emailHeader : TextView = findViewById(R.id.tvEmailHeader)
        val emailSection : LinearLayout = findViewById(R.id.emailSection)
        val notifyHeader: TextView = findViewById(R.id.tvNotificationsHeader)
        val notifySection: LinearLayout = findViewById(R.id.notificationsSection)
        val groupHeader: TextView = findViewById(R.id.tvGroupHeader)
        val groupSection: LinearLayout = findViewById(R.id.groupSection)
        val memberHeader: TextView = findViewById(R.id.tvMemberHeader)
        val memberSection: LinearLayout = findViewById(R.id.memberSection)
        val deleteHeader: TextView = findViewById(R.id.tvDeleteHeader)
        val deleteSection: LinearLayout = findViewById(R.id.deleteSection)


        var email = prefs.getString("email", "null") ?: "null"
        val token = prefs.getString("token", "null") ?: "null"

        loadCurrentSettings()

        emailHeader.setOnClickListener { emailSection.isVisible = !emailSection.isVisible }
        verifyHeader.setOnClickListener {
            if (!prefs.getString("email", "null").equals("null")) {
                verifySection.isVisible = !verifySection.isVisible
            } else {
                Toast.makeText(this, "E-Mail muss erst hinzugefügt werden", Toast.LENGTH_SHORT).show()
            }
        }
        notifyHeader.setOnClickListener {
            if (prefs.getBoolean("verify", false)) {
                notifySection.isVisible = !notifySection.isVisible
            } else {
                Toast.makeText(this, "E-Mail muss erst verifiziert werden", Toast.LENGTH_SHORT).show()
            }
        }
        groupHeader.setOnClickListener {
            if (!prefs.getString("email", "null").equals("null")) {
                groupSection.isVisible = !groupSection.isVisible
            } else {
                Toast.makeText(this, "E-Mail muss erst hinzugefügt werden", Toast.LENGTH_SHORT).show()
            }
        }
        memberHeader.setOnClickListener {
            if (!prefs.getString("email", "null").equals("null")) {
                showMembers()
                memberSection.isVisible = !memberSection.isVisible
            } else {
                Toast.makeText(this, "E-Mail muss erst hinzugefügt werden", Toast.LENGTH_SHORT).show()
            }
        }
        deleteHeader.setOnClickListener {
            deleteSection.isVisible = !deleteSection.isVisible
        }

        btnSaveEmail.setOnClickListener {
            val etemail = editTextEmail.text.toString().trim()
            if (isValidEmail(etemail)) {
                saveEmail(etemail, prefs)
            } else {
                Toast.makeText(this, "Bitte gültige E-Mail-Adresse eingeben", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        btnRequestCode.setOnClickListener {
            email = prefs.getString("email", "null") ?: "null"
            if (email != "null" || token != "null") {
                requestVerificationCode(email, token, prefs)
            } else {
                Toast.makeText(this, "Bitte gültige E-Mail-Adresse speichern", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        btnVerify.setOnClickListener {
            val code = editTextVerificationCode.text.toString().trim()
            if (code.length == 6) {
                verifyCode(email, code, token, prefs)
            } else {
                Toast.makeText(this, "Bitte gültigen 6-stelligen Code eingeben", Toast.LENGTH_SHORT)
                    .show()
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
                Toast.makeText(this, "E-Mail muss erst verifiziert werden", Toast.LENGTH_SHORT)
                    .show()
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

            inviteGroup(prefs, email, this)
            etJoinCode.text.clear()
        }

        btnJoin.setOnClickListener {
            val code = etJoinCode.text.toString()
            if (code.isEmpty() || code.length != 6) {
                Toast.makeText(this, "Bitte gültigen Einladungscode eingeben", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            joinGroup(prefs, code, this)
            etJoinCode.text.clear()
        }

        btnLeave.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Gruppe verlassen")
                .setMessage("Bist du sicher, dass du die Datengruppe verlassen möchtest?")
                .setPositiveButton("Ja") { _, _ ->
                    leaveGroup(prefs, this)
                    showMembers()
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }

        btnDeleteData.setOnClickListener {
            deleteConfirmation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadCurrentSettings() {
        Thread {
            try {
                // Email anzeigen
                val email = prefs.getString("email", "null") ?: "null"
                if (email != "null") {
                    runOnUiThread { editTextEmail.setText(email) }
                } else {
                    runOnUiThread { editTextEmail.text.clear() }
                }

                // Verifizierungsstatus anzeigen
                val verified = prefs.getBoolean("verify", false)
                val notify = prefs.getBoolean("notify", false)
                runOnUiThread {
                    if (verified) {
                        switchNotifications.isEnabled = true
                        btnVerify.isEnabled = false
                        btnRequestCode.isEnabled = false
                        editTextVerificationCode.isEnabled = false
                    } else {
                        switchNotifications.isEnabled = false
                        btnVerify.isEnabled = true
                        btnRequestCode.isEnabled = true
                        editTextVerificationCode.isEnabled = true
                    }

                    switchNotifications.isChecked = notify
                    colorVerifyText(verified)
                }
            } catch (e: Exception) {
                Log.e("loadSett", "Error: $e")
            }
        }.start() // Ende Thread
    }

    private fun requestVerificationCode(email: String, token: String, prefs: SharedPreferences) {
        Thread {
            try {
                val conn = URL(
                    "$BASE_URL/requestVerificode?email=${
                        URLEncoder.encode(
                            email,
                            "UTF-8"
                        )
                    }&token=$token"
                ).openConnection() as HttpURLConnection

                disableButton(btnRequestCode, "Erneut senden in ", "Code erneut anfordern", 60)

                conn.requestMethod = "POST"
                val responseCode = conn.responseCode

                runOnUiThread {
                    when (responseCode) {
                        200 -> {
                            Toast.makeText(
                                this,
                                "Verifizierungscode wurde gesendet",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadCurrentSettings()
                        }
                        401 -> {
                            Toast.makeText(
                                this,
                                "Fehlende Daten",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                "Fehler beim Senden des Codes$responseCode",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
                val url = URL("$BASE_URL/verifyCode")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "email=${URLEncoder.encode(email, "UTF-8")}&code=${
                    URLEncoder.encode(
                        code,
                        "UTF-8"
                    )
                }&token=$token"
                conn.outputStream.use { os ->
                    os.write(postData.toByteArray(StandardCharsets.UTF_8))
                }

                val responseCode = conn.responseCode
                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "E-Mail erfolgreich verifiziert", Toast.LENGTH_SHORT)
                            .show()
                        prefs.edit { putBoolean("verify", true) }
                        loadCurrentSettings()
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

    private fun setNotificationPreference(
        email: String,
        notify: Boolean,
        token: String,
        prefs: SharedPreferences
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/setNotifyPreference")
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
                        Toast.makeText(
                            this,
                            "Benachrichtigungseinstellungen gespeichert",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Fehler beim Speichern der Einstellungen",
                            Toast.LENGTH_SHORT
                        ).show()
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

                val formBody = FormBody.Builder()
                    .add("email", email)
                    .add("id", id.toString())
                    .add("token", token ?: "")
                    .build()

                val request = Request.Builder()
                    .url("$BASE_URL/setEmail")
                    .post(formBody)
                    .build()

                val response =  client.newCall(request).execute()

                if (response.isSuccessful) prefs.edit {
                    putString("email", email)
                    putBoolean("verify", false)
                    putBoolean("notify", false)
                }

                runOnUiThread {
                    when (response.code) {
                        200 -> {
                            Toast.makeText(this, "E-Mail-Adresse gespeichert", Toast.LENGTH_SHORT)
                                .show()
                            syncWithServer()
                            loadCurrentSettings()
                        }

                        409 -> Toast.makeText(
                            this,
                            "Diese E-Mail wird bereits verwendet",
                            Toast.LENGTH_SHORT
                        ).show()

                        401 -> Toast.makeText(this, "Ungültiger Token", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    @SuppressLint("SetTextI18n")
    private fun showMembers() {
        Thread{
            val token = prefs.getString("token", "null")
            val id = prefs.getInt("app_id", -1)
            val url = "${BASE_URL}/datagroupMembers?id=$id&token=$token"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful){
                if (response.body != null) {
                    val gson = Gson()
                    val listType = object : TypeToken<List<Map<String, List<String>>>>() {}.type
                    val data: List<Map<String, List<String>>> = gson.fromJson(response.body?.charStream(), listType)

                    val owner = data.firstOrNull { it.containsKey("owner") }?.get("owner")?.firstOrNull()?.toBoolean() ?: false
                    val members = data.firstOrNull { it.containsKey("members") }?.get("members") ?: emptyList()

                    runOnUiThread {
                        val recyclerView : RecyclerView = findViewById(R.id.recyclerViewMembers)

                        recyclerView.layoutManager = LinearLayoutManager(this)
                        recyclerView.adapter = MemberAdapter(members, owner)

                        val header = findViewById<TextView>(R.id.tvMemberHeader)
                        header.text = "Mitglieder:"
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Fehler beim Laden der Mitglieder", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    private fun colorVerifyText(verified: Boolean) {
        val statusText : String
        val color : Int
        val start : Int
        val end : Int

        if(verified) {
            statusText = "Verifizierungsstatus: Verifiziert"
            start = statusText.indexOf("Verifiziert")
            end = start + "Verifiziert".length
            color = Color.GREEN
        } else {
            statusText = "Verifizierungsstatus: Nicht verifiziert"
             start = statusText.indexOf("Nicht verifiziert")
            end = start + "Nicht verifiziert".length
            color = Color.RED
        }
        val spannable = SpannableString(statusText)

        spannable.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvVerificationStatus.text = spannable
    }

    private fun deleteConfirmation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Daten löschen")
        builder.setMessage("Möchtest du deine Daten wirklich entgültig löschen?")
        var pos = false

        builder.setPositiveButton(

            "OK",
            DialogInterface.OnClickListener {
                dialog: DialogInterface?, which: Int ->
                if(pos){
                    pos = false

                    delete()
                } else {
                    builder.setMessage("Wirklich sicher?")
                    builder.show()
                    pos = true
                }

            }
        )

        builder.setNegativeButton("Abbrechen",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                dialog!!.cancel()
            }
        )

        builder.show()
    }

    private fun delete() {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)
                val client = OkHttpClient()

                val formBody = FormBody.Builder()
                    .add("id", id.toString())
                    .add("token", token ?: "")
                    .build()

                val request = Request.Builder()
                    .url("$BASE_URL/deleteAcc")
                    .post(formBody)
                    .build()

                val response =  client.newCall(request).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "Daten gelöscht", Toast.LENGTH_SHORT).show()
                        prefs.edit {
                            putString("token", null)
                            putInt("app_id", -1)
                            putString("email", null)
                            apply()
                        }
                        syncWithServer {
                            loadCurrentSettings()
                        }
                    } else {
                        Toast.makeText(this, "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}