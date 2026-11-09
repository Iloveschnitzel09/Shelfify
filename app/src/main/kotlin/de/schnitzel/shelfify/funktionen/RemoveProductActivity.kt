package de.schnitzel.shelfify.funktionen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.funktionen.sub.BarcodeScannerActivity
import de.schnitzel.shelfify.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class RemoveProductActivity : AppCompatActivity() {

    private lateinit var editTextEan: EditText
    private lateinit var editTextName: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var buttonRemove: Button

    private val client = OkHttpClient()

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val ean = result.data?.getStringExtra("ean")
                if (!ean.isNullOrEmpty()) {
                    editTextEan.setText(ean)
                    lookupProductName(ean)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_removeproduct)

        initViews()
        setupListeners()

        if (savedInstanceState == null) {
            startScannerOrFocus()
        }
    }

    private fun initViews() {
        editTextEan = findViewById(R.id.etEan)
        editTextName = findViewById(R.id.etName)
        editTextQuantity = findViewById(R.id.etQuantity)
        buttonRemove = findViewById(R.id.btnCheckEan)

        // Der Name wird nur angezeigt, nicht bearbeitet
        editTextName.isEnabled = false
    }

    private fun setupListeners() {
        editTextEan.setOnClickListener { openScanner() }

        buttonRemove.setOnClickListener {
            val ean = editTextEan.text.toString().trim()
            if (ean.isEmpty()) {
                toast("Bitte EAN eingeben")
                return@setOnClickListener
            }

            val quantity = editTextQuantity.text.toString().trim().ifEmpty { "1" }.toIntOrNull()
            if (quantity == null || quantity < 1) {
                toast("Bitte eine gültige Menge eingeben")
                return@setOnClickListener
            }

            removeProduct(ean, quantity)
        }
    }

    private fun openScanner() {
        barcodeLauncher.launch(Intent(this, BarcodeScannerActivity::class.java))
    }

    private fun startScannerOrFocus() {
        if (prefs.getBoolean("start_with_scanner", true)) {
            openScanner()
        } else {
            editTextEan.requestFocus()
        }
    }

    private fun resetAndRestart() {
        editTextEan.text.clear()
        editTextName.text.clear()
        editTextQuantity.setText("1")
        startScannerOrFocus()
    }

    private fun lookupProductName(ean: String) {
        val accessToken = prefs.getString("accessToken", "")

        val request = Request.Builder()
            .url("$BASE_URL/lookupProductName?ean=$ean")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        lifecycleScope.launch {
            try {
                val (isSuccessful, code, bodyString) = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        Triple(response.isSuccessful, response.code, response.body?.string() ?: "")
                    }
                }

                if (isSuccessful) {
                    editTextName.setText(bodyString)
                    toast("Produktname gefunden")
                } else {
                    editTextName.text.clear()
                    when (code) {
                        404 -> toast("Produktname nicht gefunden")
                        502 -> toast("Netzwerkfehler")
                        else -> toast("Fehler: $code")
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoveProduct", "Fehler beim Abrufen des Produktnamens", e)
                toast("Fehler beim Abrufen")
            }
        }
    }

    private fun removeProduct(ean: String, quantity: Int) {
        val accessToken = prefs.getString("accessToken", "")

        val body = FormBody.Builder()
            .add("ean", ean)
            .add("quantity", quantity.toString())
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/removeProduct")
            .addHeader("Authorization", "Bearer $accessToken")
            .delete(body)
            .build()

        // Doppeltes Absenden verhindern
        buttonRemove.isEnabled = false

        lifecycleScope.launch {
            try {
                val responseCode = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response -> response.code }
                }

                when (responseCode) {
                    200 -> {
                        toast("Produkt entfernt")
                        resetAndRestart()
                    }
                    404 -> toast("Produkt nicht gefunden")
                    502 -> toast("Netzwerkfehler")
                    else -> toast("Fehler beim Entfernen (Code: $responseCode)")
                }
            } catch (e: Exception) {
                Log.e("RemoveProduct", "Fehler beim Entfernen des Produkts", e)
                toast("Fehler beim Entfernen")
            } finally {
                buttonRemove.isEnabled = true
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
