package de.schnitzel.shelfify

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.funktionen.AddProductActivity
import de.schnitzel.shelfify.funktionen.AiRecipesActivity
import de.schnitzel.shelfify.funktionen.RemoveProductActivity
import de.schnitzel.shelfify.funktionen.SettingsActivity
import de.schnitzel.shelfify.funktionen.ShowAllProductsActivity
import de.schnitzel.shelfify.funktionen.ShowAllSpoiledProductsActivity
import de.schnitzel.shelfify.util.disableButton
import de.schnitzel.shelfify.util.syncWithServer
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
        syncWithServer()

        // Buttons aus Layout
        val btnShowAll: Button = findViewById(R.id.btnShowAllProducts)
        val btnSearch: Button = findViewById(R.id.btnSearchSpoiledProducts)
        val btnAdd: Button = findViewById(R.id.btnAddProduct)
        val btnRemove: Button = findViewById(R.id.btnRemoveProduct)
        val btnSettings: Button = findViewById(R.id.btnSettings)
        val btnRecipes: Button = findViewById(R.id.btnRecipes)
        val btnSync: Button = findViewById(R.id.btnSync)

        // Klick-Events
        btnShowAll.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ShowAllProductsActivity::class.java
                )
            )
        }
        btnSearch.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ShowAllSpoiledProductsActivity::class.java
                )
            )
        }
        btnAdd.setOnClickListener { startActivity(Intent(this, AddProductActivity::class.java)) }
        btnRemove.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RemoveProductActivity::class.java
                )
            )
        }
        btnRecipes.setOnClickListener { startActivity(Intent(this, AiRecipesActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        btnSync.setOnClickListener {
            disableButton(btnSync, null, "↻", 5)
            syncWithServer()
        }
    }
}
