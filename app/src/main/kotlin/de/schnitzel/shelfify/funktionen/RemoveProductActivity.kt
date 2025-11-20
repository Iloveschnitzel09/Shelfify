package de.schnitzel.shelfify.funktionen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.funktionen.sub.BarcodeScannerActivity
import de.schnitzel.shelfify.prefs
import okhttp3.OkHttpClient
import okhttp3.Request

class RemoveProductActivity : AppCompatActivity() {
    private lateinit var editTextEan: EditText
    private lateinit var editTextName: EditText
    private lateinit var editTextQuantity: EditText

    private var barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ean = result.data?.getStringExtra("ean") ?: "null"
                editTextEan.setText(ean)
                if(ean != "null") getProName(ean)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, BarcodeScannerActivity::class.java)
        barcodeLauncher.launch(intent)

        setContentView(R.layout.activity_removeproduct)

        editTextEan = findViewById(R.id.etEan)
        editTextName = findViewById(R.id.etName)
        editTextQuantity = findViewById(R.id.etQuantity)
        val buttonCheckEan = findViewById<Button>(R.id.btnCheckEan)

        editTextEan.setOnClickListener {
            barcodeLauncher.launch(intent)
        }

        editTextEan.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                barcodeLauncher.launch(intent)
        }

        buttonCheckEan.setOnClickListener {
            val ean = editTextEan.getText().toString()
            val quantity= editTextQuantity.text.toString()
            if (!ean.isEmpty()) {
                removeProduct(ean, quantity)
            } else {
                Toast.makeText(this, "Bitte EAN eingeben", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getProName(ean: String) {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)
                val url = "${BASE_URL}/lookupProductName?ean=$ean&id=$id&token=$token"

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val name = response.body?.string() ?: ""
                        runOnUiThread {
                            editTextName.setText(name)
                            Toast.makeText(this, "Produktname gefunden", Toast.LENGTH_SHORT).show()
                            editTextName.isEnabled = false
                        }
                    } else {
                        runOnUiThread {
                            when (response.code) {
                                404 -> Toast.makeText(this, "Produktname nicht gefunden – bitte eingeben", Toast.LENGTH_SHORT).show()
                                502 -> Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                                else -> Toast.makeText(this, "Unerwarteter Fehler: ${response.code}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoveProduct", "Fehler beim Abrufen des Produktnamens: ${e.stackTrace.toString()}")
                runOnUiThread {
                    Toast.makeText(this, "Fehler beim Abrufen", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun removeProduct(ean: String, quantity: String) {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)
                val client = OkHttpClient()

                val url = "$BASE_URL/removeProduct?ean=$ean&id=$id&token=$token&quantity=$quantity"

                val removeRequest = Request.Builder()
                    .url(url)
                    .delete()
                    .build()

                client.newCall(removeRequest).execute().use { response ->
                    runOnUiThread {
                        when (response.code) {
                            200 -> {
                                Toast.makeText(this, "Produkt entfernt", Toast.LENGTH_SHORT).show()
                                editTextEan.text.clear()
                                editTextName.text.clear()
                                editTextQuantity.text.clear()
                            }
                            404 -> Toast.makeText(this, "Produkt nicht gefunden", Toast.LENGTH_SHORT).show()
                            502 -> Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this, "Fehler beim Entfernen (Code: ${response.code})", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoveProduct", e.stackTrace.toString())
                runOnUiThread {
                    Toast.makeText(this, "Fehler beim Entfernen", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}