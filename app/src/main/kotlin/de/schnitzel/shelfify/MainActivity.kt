package de.schnitzel.shelfify

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.funktionen.AddProductActivity
import de.schnitzel.shelfify.funktionen.AiRecipesActivity
import de.schnitzel.shelfify.funktionen.RemoveProductActivity
import de.schnitzel.shelfify.funktionen.SettingsActivity
import de.schnitzel.shelfify.funktionen.ShowAllProductsActivity
import de.schnitzel.shelfify.funktionen.ShowAllSpoiledProductsActivity

lateinit var prefs: SharedPreferences

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        findViewById<Button>(R.id.btnShowAllProducts).setOnClickListener { startActivity(Intent(this, ShowAllProductsActivity::class.java)) }
        findViewById<Button>(R.id.btnSearchSpoiledProducts).setOnClickListener { startActivity(Intent(this, ShowAllSpoiledProductsActivity::class.java)) }
        findViewById<Button>(R.id.btnAddProduct).setOnClickListener { startActivity(Intent(this, AddProductActivity::class.java)) }
        findViewById<Button>(R.id.btnRemoveProduct).setOnClickListener { startActivity(Intent(this, RemoveProductActivity::class.java)) }
        findViewById<Button>(R.id.btnRecipes).setOnClickListener { startActivity(Intent(this, AiRecipesActivity::class.java)) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
}
