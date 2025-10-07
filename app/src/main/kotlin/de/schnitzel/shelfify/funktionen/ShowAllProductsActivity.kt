package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.api.productRequest
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.Products
import de.schnitzel.shelfify.util.adapter.ProductAdapter
import okhttp3.OkHttpClient
import okhttp3.Request

class ShowAllProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all)

        val etSearch : EditText = findViewById(R.id.etSearch)
        val btnSearch: Button = findViewById(R.id.btnSearch)

        val token = prefs.getString("token", "null")
        val id = prefs.getInt("app_id", -1)
        val url = "${BASE_URL}/products?id=$id&token=$token"

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
            val searchTerm = etSearch.text.toString().lowercase()
            for (product in products ?: emptyList()) {
                if (product.produktname.lowercase().contains(searchTerm)) {
                    val index = products?.indexOf(product) ?: -1
                    Log.e("SEARCH", "Index: $index")
                    if (index != -1) {
                        recyclerView.smoothScrollToPosition(index)
                        return@setOnClickListener
                    }
                }
            }
            Toast.makeText(this, "Produkt nicht gefunden!", Toast.LENGTH_SHORT).show()
        }
    }
}