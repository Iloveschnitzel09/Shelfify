package de.schnitzel.shelfify

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.funktionen.AddProductActivity
import de.schnitzel.shelfify.funktionen.RemoveProductActivity
import de.schnitzel.shelfify.funktionen.SettingsActivity
import de.schnitzel.shelfify.funktionen.ShowAllProductsActivity
import de.schnitzel.shelfify.funktionen.ShowAllSpoiledProductsActivity
import de.schnitzel.shelfify.util.disableButton
import de.schnitzel.shelfify.util.syncWithServer
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

lateinit var prefs: SharedPreferences
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // App-Synchronisation beim Start
        syncWithServer(this)

        // Buttons aus Layout
        val btnShowAll: Button = findViewById(R.id.btnShowAllProducts)
        val btnSearch: Button = findViewById(R.id.btnSearchSpoiledProducts)
        val btnAdd: Button = findViewById(R.id.btnAddProduct)
        val btnRemove: Button = findViewById(R.id.btnRemoveProduct)
        val btnSettings: Button = findViewById(R.id.btnSettings)
        val btnSync: Button = findViewById(R.id.btnSync)

        // Klick-Events
        btnShowAll.setOnClickListener { startActivity(Intent(this, ShowAllProductsActivity::class.java)) }
        btnSearch.setOnClickListener { startActivity(Intent(this, ShowAllSpoiledProductsActivity::class.java)) }
        btnAdd.setOnClickListener { startActivity(Intent(this, AddProductActivity::class.java)) }
        btnRemove.setOnClickListener { startActivity(Intent(this, RemoveProductActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        btnSync.setOnClickListener {
            disableButton(btnSync, "x", "↻", 5)
            syncWithServer(this)
        }
    }

    private fun delete() {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)

                val url = URL("${ApiConfig.BASE_URL}/deleteAcc")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "id=${URLEncoder.encode(id.toString(), StandardCharsets.UTF_8)}&token=$token"
                } else null

                conn.outputStream.use { os ->
                    postData?.let { os.write(it.toByteArray(StandardCharsets.UTF_8)) }
                }

                val responseCode = conn.responseCode
                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Daten gelöscht", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
