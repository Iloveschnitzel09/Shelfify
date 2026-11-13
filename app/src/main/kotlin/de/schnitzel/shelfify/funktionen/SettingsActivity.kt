package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiClient
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.api.syncWithServer
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.inviteGroup
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.joinGroup
import de.schnitzel.shelfify.funktionen.sub.DatagroupService.leaveGroup
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.adapter.MemberAdapter
import de.schnitzel.shelfify.util.disableButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
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
    private lateinit var btnDeleteData : Button

    private val client = ApiClient.client

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

        val verifyHeader : MaterialCardView = findViewById(R.id.tvVerificationHeader)
        val verifySection : LinearLayout = findViewById(R.id.verificationSection)
        val emailHeader : MaterialCardView = findViewById(R.id.tvEmailHeader)
        val emailSection : LinearLayout = findViewById(R.id.emailSection)
        val notifyHeader: MaterialCardView = findViewById(R.id.tvNotificationsHeader)
        val notifySection: LinearLayout = findViewById(R.id.notificationsSection)
        val groupHeader: MaterialCardView = findViewById(R.id.tvGroupHeader)
        val groupSection: LinearLayout = findViewById(R.id.groupSection)
        val memberHeader: MaterialCardView = findViewById(R.id.tvMemberHeader)
        val memberSection: LinearLayout = findViewById(R.id.memberSection)
        val deleteHeader: MaterialCardView = findViewById(R.id.tvDeleteHeader)
        val deleteSection: LinearLayout = findViewById(R.id.deleteSection)


        var email = prefs.getString("email", "null") ?: "null"
        val token = prefs.getString("token", "null") ?: "null"

        loadCurrentSettings()

        emailHeader.setOnClickListener { emailSection.isVisible = !emailSection.isVisible }
        verifyHeader.setOnClickListener {
            if (!prefs.getString("email", "null").equals("null")) {
                verifySection.isVisible = !verifySection.isVisible
            } else {
                toast("E-Mail muss erst hinzugefügt werden")
            }
        }
        notifyHeader.setOnClickListener {
            if (prefs.getBoolean("verify", false)) {
                notifySection.isVisible = !notifySection.isVisible
            } else {
                toast("E-Mail muss erst verifiziert werden")
            }
        }
        groupHeader.setOnClickListener {
            if (!prefs.getString("email", "null").equals("null")) {
                groupSection.isVisible = !groupSection.isVisible
            } else {
                toast("E-Mail muss erst hinzugefügt werden")
            }
        }
        memberHeader.setOnClickListener {
            if (!prefs.getString("email", "null").equals("null")) {
                showMembers()
                memberSection.isVisible = !memberSection.isVisible
            } else {
                toast("E-Mail muss erst hinzugefügt werden")
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
                toast("Bitte gültige E-Mail-Adresse eingeben")
            }
        }

        btnRequestCode.setOnClickListener {
            email = prefs.getString("email", "null") ?: "null"
            if (email != "null" || token != "null") {
                requestVerificationCode(email, token)
            } else {
                toast("Bitte gültige E-Mail-Adresse speichern")
            }
        }

        btnVerify.setOnClickListener {
            val code = editTextVerificationCode.text.toString().trim()
            if (code.length == 6) {
                verifyCode(email, code, token, prefs)
            } else {
                toast("Bitte gültigen 6-stelligen Code eingeben")
            }
        }

        switchNotifications.setOnClickListener { _ ->
            if (prefs.getBoolean("verify", false)) {
                setNotificationPreference(email, true, token, prefs)
                prefs.edit { putBoolean("verify", true) }
            } else {
                switchNotifications.isChecked = false
                setNotificationPreference(email, false, token, prefs)
                prefs.edit { putBoolean("verify", false) }
                toast("E-Mail muss erst verifiziert werden")
            }
        }

        btnInvite.setOnClickListener {
            disableButton(btnInvite, " Warte ", " Einladen ", 30)

            val email = etInviteEmail.text.toString().trim()
            if (!isValidEmail(email)) {
                toast("Bitte gültige E-Mail-Adresse eingeben")
                return@setOnClickListener
            }

            inviteGroup(prefs, email, this)
            etJoinCode.text.clear()
        }

        btnJoin.setOnClickListener {
            val code = etJoinCode.text.toString()
            if (code.isEmpty() || code.length != 6) {
                toast("Bitte gültigen Einladungscode eingeben")
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

        findViewById<TextView>(R.id.textViewVersion).text = getAppVersionText()
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
                Log.e("SettingsActivity", e.stackTrace.contentToString())
            }
        }.start() // Ende Thread
    }

    private fun requestVerificationCode(email: String, token: String) {
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
                            toast("Verifizierungscode wurde gesendet",)
                            loadCurrentSettings()
                        }
                        401 -> {
                            toast("Ungültige Anmeldedaten")
                        }
                        502 -> {
                            toast("Netzwerkfehler")
                        }
                        else -> {
                            toast("Fehler beim Senden des Codes: $responseCode")
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SettingsActivity", e.stackTrace.contentToString())
                runOnUiThread { toast("Netzwerkfehler") }
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
                    when (responseCode) {
                        200 -> {
                            toast("E-Mail erfolgreich verifiziert")
                            prefs.edit { putBoolean("verify", true) }
                            loadCurrentSettings()
                        }
                        400 -> toast("Ungültiger Code")
                        502 -> toast("Netzwerkfehler")
                        else -> toast("Fehler bei der Verifizierung (Code: $responseCode)")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SettingsActivity", e.stackTrace.contentToString())
                runOnUiThread { toast("Netzwerkfehler") }
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
                    when (responseCode) {
                        200 -> {
                            prefs.edit { putBoolean("notify", notify) }
                            toast("Benachrichtigungseinstellungen gespeichert")
                        }
                        401 -> {
                            toast("Ungültige Anmeldedaten")
                            switchNotifications.isChecked = !notify
                        }
                        502 -> {
                            toast("Netzwerkfehler")
                            switchNotifications.isChecked = !notify
                        }
                        else -> {
                            toast("Fehler beim Speichern der Einstellungen (Code: $responseCode)")
                            switchNotifications.isChecked = !notify
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SettingsActivity", e.stackTrace.contentToString())
                runOnUiThread {
                    toast("Netzwerkfehler")
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
                            toast("E-Mail-Adresse gespeichert")
                            //syncWithServer()
                            loadCurrentSettings()
                        }
                        409 -> Toast.makeText(
                            this,
                            "Diese E-Mail wird bereits verwendet",
                            Toast.LENGTH_SHORT
                        ).show()
                        401 -> toast("Ungültiger Token")
                        502 -> toast("Netzwerkfehler")
                        else -> toast("Fehler beim Speichern (Code: ${response.code})")
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", e.stackTrace.contentToString())
                runOnUiThread { toast("Netzwerkfehler") }
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
                    }
                }
            } else {
                runOnUiThread {
                    when (response.code) {
                        401 -> toast("Ungültige Anmeldedaten")
                        502 -> toast("Netzwerkfehler")
                        else -> toast("Fehler beim Laden der Mitglieder (Code: ${response.code})")
                    }
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

            "OK"
        ) { _,_ ->
            if (pos) {
                pos = false

                delete()
            } else {
                builder.setMessage("Wirklich sicher?")
                builder.show()
                pos = true
            }

        }

        builder.setNegativeButton("Abbrechen"
        ) { dialog: DialogInterface?, _ ->
            dialog!!.cancel()
        }

        builder.show()
    }

    private fun delete() {
        val accessToken = prefs.getString("accessToken", "")
        val request = Request.Builder()
            .url("$BASE_URL/deleteAcc")
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        lifecycleScope.launch {
            try {
                val responseCode = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response -> response.code }
                }

                when (responseCode) {
                    200 -> {
                        toast("Daten gelöscht")
                        prefs.edit {
                            clear()
                            apply()
                        }
                        syncWithServer(prefs)
                    }
                    401 -> toast("Ungültige Anmeldedaten")
                    502 -> toast("Netzwerkfehler")
                    else -> toast("Fehler beim Entfernen (Code: $responseCode)")
                }
            } catch (e: Exception) {
                Log.e("RemoveProduct", "Fehler beim Entfernen des Produkts", e)
                toast("Fehler beim Entfernen")
            }
        }
    }

    private fun getAppVersionText(): String {
        val info: PackageInfo = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
        val versionCode = PackageInfoCompat.getLongVersionCode(info)
        return "Version ${info.versionName} (Build $versionCode)"
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}