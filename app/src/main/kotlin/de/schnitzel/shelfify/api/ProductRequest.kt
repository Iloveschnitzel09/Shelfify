package de.schnitzel.shelfify.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.util.ProductAdapter
import de.schnitzel.shelfify.util.Products
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
fun productRequest(request: Request, client: OkHttpClient): ProductAdapter? {
    try {
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val jsonResponse = response.body?.string()
            val listType = object : TypeToken<List<Products>>() {}.type
            val products = Gson().fromJson<List<Products>>(jsonResponse, listType)
            Log.e("PR", "Es wurden ${products?.size ?: "null"} Produkte geladen.")
            return ProductAdapter(products ?: emptyList())
        } else {
            Log.e("PR", "Anfragen fehler. ${response.code}")
            return null
        }
    } catch (e: IOException) {
        Log.e("PR", "Scheißendreck: ${e.message}")
        return null
    }
}
