package de.schnitzel.shelfify.api

import android.content.SharedPreferences

import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.util.Products
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val syncClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .callTimeout(10, TimeUnit.SECONDS)
    .build()

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

suspend fun syncWithServer(prefs: SharedPreferences): Boolean = withContext(Dispatchers.IO) {
    try {
        val savedRefreshToken = prefs.getString("refreshToken", null)

        if (savedRefreshToken != null) {
            val body = """{"refreshToken":"$savedRefreshToken"}""".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$BASE_URL/refreshToken")
                .post(body)
                .build()

            syncClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext parseAndSaveUserData(response.body?.string(), prefs)
                }

                Log.e("Sync", "Refresh fehlgeschlagen: Code ${response.code}")
                return@withContext false
            }
        }

        registerNewUser(prefs)
    } catch (e: Exception) {
        Log.e("Sync", "Server nicht erreichbar: ${e.message}")
        false
    }
}

private fun registerNewUser(prefs: SharedPreferences): Boolean {
    val request = Request.Builder().url("$BASE_URL/appSync").get().build()

    return syncClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            parseAndSaveUserData(response.body?.string(), prefs)
        } else {
            Log.e("Sync", "appSync fehlgeschlagen: Code ${response.code}")
            false
        }
    }
}

private fun parseAndSaveUserData(jsonStr: String?, prefs: SharedPreferences): Boolean {
    return !jsonStr.isNullOrEmpty() && try {
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
        true
    } catch (e: JSONException) {
        // z. B. Proxy liefert 200 mit HTML statt JSON
        Log.e("Sync", "Unerwartete Server-Antwort: ${e.message}")
        false
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
