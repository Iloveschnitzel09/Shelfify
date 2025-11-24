package de.schnitzel.shelfify.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.util.Products
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

fun productRequest(request: Request, client: OkHttpClient): List<Products>? {
    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val jsonResponse = response.body?.string()
            val listType = object : TypeToken<List<Products>>() {}.type
            val products = Gson().fromJson<List<Products>>(jsonResponse, listType)
            Log.i("ProductRequest", "Erfolgreich ${products?.size ?: 0} Produkte geladen")
            return products
        } else {
            Log.e("ProductRequest", "HTTP-Fehler beim Laden der Produkte: ${response.code}")
            return null
        }
    } catch (e: IOException) {
        Log.e("ProductRequest", "Netzwerkfehler beim Laden der Produkte: ${e.message}")
        return null
    }
}
