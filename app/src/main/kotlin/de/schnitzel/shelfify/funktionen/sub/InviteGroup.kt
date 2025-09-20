import android.app.Activity
import android.content.SharedPreferences
import android.widget.Toast
import de.schnitzel.shelfify.api.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val baseUrl = ApiConfig.BASE_URL
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
                "$baseUrl/inviteToDatagroup?id=$id&token=$token&email=$email"
            ).openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            val code = conn.responseCode
            conn.disconnect()

            withContext(Dispatchers.Main) {
                when (code) {
                    200 -> Toast.makeText(activity, "Einladung erfolgreich!", Toast.LENGTH_SHORT).show()
                    400 -> Toast.makeText(activity, "Ungültige Anfrage!", Toast.LENGTH_SHORT).show()
                    401 -> Toast.makeText(activity, "Token ungültig!", Toast.LENGTH_SHORT).show()
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
