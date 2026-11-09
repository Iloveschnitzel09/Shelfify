package de.schnitzel.shelfify.funktionen

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL

import de.schnitzel.shelfify.util.Products
import de.schnitzel.shelfify.util.adapter.ProductAdapter
import de.schnitzel.shelfify.util.productRequest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class ShowAllProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var prefs: SharedPreferences

    private var loadedProducts: List<Products> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all)

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val etSearch : EditText = findViewById(R.id.etSearch)
        val btnSearch: Button = findViewById(R.id.btnSearch)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val accessToken = prefs.getString("accessToken", "")
        val url = "$BASE_URL/products"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $accessToken").build()

        lifecycleScope.launch {
            val result = productRequest(request, client)
            if (result != null) {
                loadedProducts =result
                recyclerView.adapter = ProductAdapter(loadedProducts)
            } else {
                Toast.makeText(this@ShowAllProductsActivity, "Fehler beim Laden der Produkte!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSearch.setOnClickListener {
            val searchTerm = etSearch.text.toString().lowercase()

            if(loadedProducts.isEmpty()) {
                Toast.makeText(this@ShowAllProductsActivity, "Produkte werden noch geladen, bitte warten!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val filteredList = loadedProducts.filter { product ->
                product.name.lowercase().contains(searchTerm)
            }

            if(filteredList.isEmpty()) {
                Toast.makeText(this@ShowAllProductsActivity, "Keine Produkte gefunden!", Toast.LENGTH_SHORT).show()
            } else {
                recyclerView.adapter = ProductAdapter(filteredList)
            }
        }
    }
}