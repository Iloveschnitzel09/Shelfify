package de.schnitzel.shelfify.funktionen

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.util.ProductAdapter
import de.schnitzel.shelfify.util.Products
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ShowAllSpoiledProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all_spoiled_products)
        
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Zeitraum auswählen")
        
        val numberPicker = NumberPicker(this)
        numberPicker.minValue = 1
        numberPicker.maxValue = 365
        numberPicker.value = 7
        numberPicker.setBackgroundColor(121212)
        
        builder.setView(numberPicker)

        builder.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                val selectedDays = numberPicker.value
                loadSpoiledProducts(selectedDays) // Hier wird dein Request mit den ausgewählten Tagen gestartet
            })

        builder.setNegativeButton(
            "Abbrechen",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.cancel() })

        builder.show()
    }

    private fun loadSpoiledProducts(days: Int) {
        val url = "${ApiConfig.BASE_URL}/spoiledProducts?days=$days"

        // Netzwerk-Request mit OkHttp
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // JSON-Response auslesen
                    val jsonResponse = response.body?.string()

                    // JSON in eine Liste von Produkten umwandeln
                    val gson = Gson()
                    val listType = object : TypeToken<List<Products>>() {}.type
                    val products = gson.fromJson<List<Products>>(jsonResponse, listType)

                    runOnUiThread {
                        // RecyclerView mit den Produkten aktualisieren
                        productAdapter = ProductAdapter(products ?: emptyList())
                        recyclerView.adapter = productAdapter
                    }
                } else {
                    runOnUiThread {
                        // Fehlerbehandlung falls die Antwort nicht erfolgreich ist
                        Toast.makeText(applicationContext, "Fehler beim Laden der Produkte", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // Fehlerbehandlung bei Netzwerkproblemen
                    Toast.makeText(applicationContext, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


}