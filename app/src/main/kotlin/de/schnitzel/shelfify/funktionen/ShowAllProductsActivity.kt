package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.util.ProductAdapter
import de.schnitzel.shelfify.util.Products
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ShowAllProductsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_all)

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