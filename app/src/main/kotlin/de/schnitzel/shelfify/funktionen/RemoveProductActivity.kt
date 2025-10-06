package de.schnitzel.shelfify.funktionen

import android.content.Intent
import android.os.Bundle
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

    private var barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ean = result.data?.getStringExtra("ean")
                editTextEan.setText(ean)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, BarcodeScannerActivity::class.java)
        barcodeLauncher.launch(intent)

        setContentView(R.layout.activity_removeproduct)

        editTextEan = findViewById(R.id.etEan)
        val buttonCheckEan = findViewById<Button?>(R.id.btnCheckEan)

        editTextEan.setOnClickListener {
            barcodeLauncher.launch(intent)
        }

        editTextEan.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                barcodeLauncher.launch(intent)
        }

        buttonCheckEan?.setOnClickListener {
            val ean = editTextEan.getText().toString()
            if (!ean.isEmpty()) {
                removeProduct(ean)
            } else {
                Toast.makeText(this, "Bitte EAN eingeben", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeProduct(ean: String) {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)
                val client = OkHttpClient()

                val url = "$BASE_URL/removeProduct?ean=$ean&id=$id&token=$token"

                val removeRequest = Request.Builder()
                    .url(url)
                    .delete()
                    .build()

                val response = client.newCall(removeRequest).execute()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "Produkt entfernt", Toast.LENGTH_SHORT).show()
                        editTextEan.text.clear()
                    } else {
                        Toast.makeText(
                            this,
                            "Produkt nicht gefunden ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread(Runnable {
                    Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                })
            }
        }.start()
    }
}