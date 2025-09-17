package de.schnitzel.shelfify.funktionen

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiClient
import de.schnitzel.shelfify.api.ProductApi
import de.schnitzel.shelfify.util.adapter.ProductAdapter
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

        val api = ApiClient.client.create(ProductApi::class.java)
        api.getAllProducts().enqueue(object : Callback<List<Products>> {
            override fun onResponse(call: Call<List<Products>>, response: Response<List<Products>>) {
                if (response.isSuccessful) {
                    val produkteListe = response.body()
                    Log.d("API", "Anzahl Produkte: ${produkteListe?.size ?: "null"}")

                    adapter = ProductAdapter(produkteListe ?: emptyList())
                    recyclerView.adapter = adapter
                } else {
                    Log.e("API", "Response nicht erfolgreich: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Products>>, t: Throwable) {
                Log.e("API", "Fehler beim API-Aufruf: ", t)
            }
        })
    }
}