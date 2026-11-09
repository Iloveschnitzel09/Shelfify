package de.schnitzel.shelfify.util

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

suspend fun syncWithServer(prefs: SharedPreferences): Boolean = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val savedRefreshToken = prefs.getString("refreshToken", null)

    try {
        if (savedRefreshToken == null) {
            val request = Request.Builder().url("$BASE_URL/appSync").get().build()
            val response = client.newCall(request).execute()

            if(response.isSuccessful) {
                parseAndSaveUserData(response.body?.string(), prefs)
                return@withContext true
            }
        } else {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val body = """{"refreshToken":"$savedRefreshToken"}""".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$BASE_URL/refreshToken")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                parseAndSaveUserData(response.body?.string(), prefs)
                return@withContext true
            } else {
                Log.e("Sync", "Refresh fehlgeschlagen: Code ${response.code}")
            }
        }
    } catch (e: Exception) {
        Log.e("AppSync", e.message.toString())
        return@withContext false
    }
    return@withContext false
}

private fun parseAndSaveUserData(jsonStr: String?, prefs: SharedPreferences) {
    if (jsonStr.isNullOrEmpty()) return
    val json = JSONObject(jsonStr)

    prefs.edit {
        putString("accessToken", json.getString("accessToken"))
        putString("refreshToken", json.getString("refreshToken"))
        putInt("app_id", json.getInt("id"))

        // Optionale Felder mit Fallbacks
        putString("email", json.optString("email", ""))
        putBoolean("notify", json.optBoolean("notify", false))
        putBoolean("verify", json.optBoolean("verified", false))
    }
}

suspend fun productRequest(request: Request, client: OkHttpClient): List<Products>? =
withContext(Dispatchers.IO) {
    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val jsonResponse = response.body?.string() ?: return@withContext null
            val listType = object : TypeToken<List<Products>>() {}.type
            return@withContext Gson().fromJson(jsonResponse, listType)
        }
    } catch (e: Exception) {
        Log.e("ProductRequest", "Fehler beim Laden: ${e.message}")
    }
    return@withContext null
}
