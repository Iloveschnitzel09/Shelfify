package de.schnitzel.shelfify.funktionen

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.api.productRequest
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.Products
import de.schnitzel.shelfify.util.adapter.ProductAdapter
import okhttp3.OkHttpClient
import okhttp3.Request

class ShowAllSpoiledProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val etSearch : EditText = findViewById(R.id.etSearch)
        val btnSearch: Button = findViewById(R.id.btnSearch)

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
                loadSpoiledProducts(selectedDays, btnSearch, etSearch)
            })

        builder.setNegativeButton(
            "Abbrechen",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.cancel() }
        )

        builder.show()
    }

    private fun loadSpoiledProducts(days: Int, btnSearch: Button, etSearch: EditText) {
        val token = prefs.getString("token", "null")
        val id = prefs.getInt("app_id", -1)
        val url = "${ApiConfig.BASE_URL}/spoiledProducts?days=$days&id=$id&token=$token"

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        var products : List<Products>? = null
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        Thread {
            products = productRequest(request, client)

            val data = ProductAdapter(products ?: emptyList())
            runOnUiThread {
                recyclerView.adapter = data
            }
        }.start()

        btnSearch.setOnClickListener {
            val prolist : MutableList<Products> = mutableListOf()
            val searchTerm = etSearch.text.toString().lowercase()
            for (product in products ?: emptyList()) {
                if (product.produktname.lowercase().contains(searchTerm)) {
                    prolist.add(product)
                }
            }

            if (prolist.isEmpty()) {
                Toast.makeText(this, "Produkt nicht gefunden!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val data = ProductAdapter(prolist)
            runOnUiThread {
                recyclerView.adapter = data
            }
        }
    }
}