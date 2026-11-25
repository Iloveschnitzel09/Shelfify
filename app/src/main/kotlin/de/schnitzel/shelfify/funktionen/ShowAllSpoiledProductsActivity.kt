package de.schnitzel.shelfify.funktionen

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiClient
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.api.ConnectionGuard
import de.schnitzel.shelfify.api.productRequest
import de.schnitzel.shelfify.util.Products
import de.schnitzel.shelfify.util.adapter.ProductAdapter
import kotlinx.coroutines.launch
import okhttp3.Request

class ShowAllSpoiledProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var prefs: SharedPreferences

    private var loadedProducts: List<Products> = emptyList()
    private var selectedDays = -1
    private var loadedAtGeneration = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all)

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val etSearch: EditText = findViewById(R.id.etSearch)
        val btnSearch: Button = findViewById(R.id.btnSearch)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnSearch.setOnClickListener {
            val searchTerm = etSearch.text.toString().lowercase()

            if (loadedProducts.isEmpty()) {
                toast("Produkte werden noch geladen, bitte warten!")
                return@setOnClickListener
            }

            val filteredList = loadedProducts.filter { product ->
                product.name.lowercase().contains(searchTerm)
            }

            if (filteredList.isEmpty()) {
                toast("Keine Produkte gefunden!")
            } else {
                recyclerView.adapter = ProductAdapter(filteredList)
            }
        }

        showDaysDialog()
    }

    override fun onResume() {
        super.onResume()
        // Nach einem Verbindungsabbruch (Sperrscreen) die Liste neu laden
        if (selectedDays != -1 && loadedAtGeneration != ConnectionGuard.reconnectCount) {
            loadProducts()
        }
    }

    private fun showDaysDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wie viele Tage im voraus?")

        val numberPicker = NumberPicker(this)
        numberPicker.minValue = 1
        numberPicker.maxValue = 365
        numberPicker.value = 7
        numberPicker.setBackgroundColor(121212)

        builder.setView(numberPicker)

        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            selectedDays = numberPicker.value
            loadProducts()
        }

        builder.setNegativeButton(
            "Abbrechen"
        ) { dialog: DialogInterface?, _ -> dialog!!.cancel() }

        builder.show()
    }

    private fun loadProducts() {
        loadedAtGeneration = ConnectionGuard.reconnectCount

        // Token jedes Mal frisch lesen, der Sync kann ihn inzwischen erneuert haben
        val accessToken = prefs.getString("accessToken", "")
        val request = Request.Builder()
            .url("$BASE_URL/products?days=$selectedDays")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        lifecycleScope.launch {
            val result = productRequest(request, ApiClient.client)

            if (result != null) {
                loadedProducts = result
                recyclerView.adapter = ProductAdapter(loadedProducts)
            } else {
                toast("Fehler beim Laden der Produkte!")
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}