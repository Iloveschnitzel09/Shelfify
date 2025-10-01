package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.api.productRequest
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.adapter.ProductAdapter
import okhttp3.OkHttpClient
import okhttp3.Request

class ShowAllProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all)

        val token = prefs.getString("token", "null")
        val id = prefs.getInt("app_id", -1)
        val url = "${BASE_URL}/products?id=$id&token=$token"

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        Thread {
            val data = productRequest(request, client)
            runOnUiThread {
                recyclerView.adapter = data
            }
        }.start()
    }
}