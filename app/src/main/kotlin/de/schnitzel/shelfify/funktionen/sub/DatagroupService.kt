package de.schnitzel.shelfify.funktionen.sub

import android.app.Activity
import android.content.SharedPreferences
import android.widget.Toast
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object DatagroupService {
    fun joinGroup(prefs: SharedPreferences, invCode: String, activity: Activity) {
        val id = prefs.getInt("app_id", -1)
        val token = prefs.getString("token", null)

        if (id == -1 || token == null) {
            Toast.makeText(activity, "App-Daten fehlen!", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val conn =
                    URL("$BASE_URL/joinDatagroup?id=$id&token=$token&code=$invCode").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                val code = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    when (code) {
                        200 -> Toast.makeText(activity, "Beitritt erfolgreich!", Toast.LENGTH_SHORT).show()

                        400 -> Toast.makeText(activity, "Ungültige Anfrage!", Toast.LENGTH_SHORT).show()

                        401 -> Toast.makeText(activity, "Token ungültig!", Toast.LENGTH_SHORT).show()

                        403 -> Toast.makeText(activity, "Einladungscode ungültig!", Toast.LENGTH_SHORT).show()

                        else -> Toast.makeText(activity, "Fehler: $code", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Netzwerkfehler: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun inviteGroup(prefs: SharedPreferences, email: String, activity: Activity) {
        val id = prefs.getInt("app_id", -1)
        val token = prefs.getString("token", null)

        if (id == -1 || token == null) {
            Toast.makeText(activity, "App-Daten fehlen!", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conn = URL(
                    "$BASE_URL/inviteToDatagroup?id=$id&token=$token&email=$email"
                ).openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                val code = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    when (code) {
                        200 -> Toast.makeText(
                            activity,
                            "Einladung erfolgreich!",
                            Toast.LENGTH_SHORT
                        ).show()

                        400 -> Toast.makeText(activity, "Ungültige Email!", Toast.LENGTH_SHORT)
                            .show()

                        401 -> Toast.makeText(activity, "Token ungültig!", Toast.LENGTH_SHORT)
                            .show()

                        403 -> Toast.makeText(activity, "Blockierter Benutzer!", Toast.LENGTH_SHORT)
                            .show()

                        else -> Toast.makeText(activity, "Fehler: $code", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Netzwerkfehler: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun leaveGroup(prefs: SharedPreferences, activity: Activity) {
        val id = prefs.getInt("app_id", -1)
        val token = prefs.getString("token", null)

        if (id == -1 || token == null) {
            Toast.makeText(activity, "App-Daten fehlen!", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val conn = URL(
                    "$BASE_URL/leaveDatagroup?id=$id&token=$token"
                ).openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                val code = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    when (code) {
                        200 -> Toast.makeText(
                            activity,
                            "Verlassen erfolgreich!",
                            Toast.LENGTH_SHORT
                        ).show()

                        400 -> Toast.makeText(activity, "Ungültige Anfrage!", Toast.LENGTH_SHORT)
                            .show()

                        401 -> Toast.makeText(activity, "Token ungültig!", Toast.LENGTH_SHORT)
                            .show()

                        else -> Toast.makeText(activity, "Fehler: $code", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Netzwerkfehler: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}