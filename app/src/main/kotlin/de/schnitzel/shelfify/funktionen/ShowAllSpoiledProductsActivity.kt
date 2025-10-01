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
                loadSpoiledProducts(selectedDays)
            })

        builder.setNegativeButton(
            "Abbrechen",
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> dialog!!.cancel() })

        builder.show()
    }

    private fun loadSpoiledProducts(days: Int) {
        val token = prefs.getString("token", "null")
        val id = prefs.getInt("app_id", -1)
        val url = "${ApiConfig.BASE_URL}/spoiledProducts?days=$days&id=$id&token=$token"

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