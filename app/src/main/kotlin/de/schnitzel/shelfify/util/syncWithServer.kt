package de.schnitzel.shelfify.util

import android.util.Log
import androidx.core.content.edit
import com.android.identity.util.UUID
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.prefs
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

fun syncWithServer(onComplete: (() -> Unit)? = null) {
    Thread {
        try {
            val savedId = prefs.getInt("app_id", -1)
            var token = prefs.getString("token", null)

            if (token == null) {
                token = UUID.randomUUID().toString()
                prefs.edit { putString("token", token) }
            }

            var urlStr = "$BASE_URL/appSync"

            urlStr += if (savedId != -1) {
                "?id=$savedId&token=$token"
            } else {
                "?token=$token"
            }

            Log.i("AppSync", "id$savedId")

            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            val code = conn.responseCode
            if (code == 200) {
                val inputStream = conn.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val result = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    result.append(line)
                }

                val json = JSONObject(result.toString())
                val id = json.getInt("id")
                if (savedId != id) {
                    prefs.edit { putInt("app_id", id) }
                    Log.i("AppSync", "Neue ID gespeichert: $id")
                }

                val email = json.getString("email")
                if (email != prefs.getString("email", " ")) {
                    prefs.edit { putString("email", email) }
                    Log.i("AppSync", "Neue Email gespeichert: $email")
                }

                val notify = json.getBoolean("notify")
                if (prefs.getBoolean("notify", false) != notify) {
                    prefs.edit { putBoolean("notify", notify) }
                    Log.i("AppSync", "Notify geändert: $notify")
                }

                val verify = json.getBoolean("verified")
                if (prefs.getBoolean("verify", false) != verify) {
                    prefs.edit { putBoolean("verify", verify) }
                    Log.i("AppSync", "Verify geändert: $verify")
                }

                onComplete?.invoke()
            } else {
                Log.e("Sync", "Fehler beim Sync: Code $code")
            }

            conn.disconnect()
        } catch (e: Exception) {
            Log.e("AppSync", e.stackTrace.toString())
        }
    }.start()
}