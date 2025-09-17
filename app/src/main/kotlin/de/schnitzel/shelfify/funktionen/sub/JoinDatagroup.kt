package de.schnitzel.shelfify.funktionen.sub

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.api.ApiConfig
import java.net.HttpURLConnection
import java.net.URL

fun joinDatagroup(prefs: SharedPreferences, datagroup: String){

    val baseUrl = ApiConfig.BASE_URL

    Thread {
        try {
            val email = prefs.getString("email", "")
            val token = prefs.getString("token", "")

            val conn = URL("$baseUrl/inviteToDatagroup?email=$email&token=$token&datagroup?=$datagroup").openConnection() as HttpURLConnection

            conn.requestMethod =  "POST"
            val code = conn.responseCode
            if (code == 200){

            }

        } catch (e: Exception) {

        }
    }.start()
}