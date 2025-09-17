package de.schnitzel.shelfify.funktionen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.funktionen.sub.BarcodeScannerActivity
import java.net.HttpURLConnection
import java.net.URL

class RemoveProductActivity : AppCompatActivity() {
    private lateinit var editTextEan: EditText

    private var barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        editTextEan.setOnClickListener(View.OnClickListener { v: View? ->
            barcodeLauncher.launch(intent)
        })

        buttonCheckEan?.setOnClickListener(View.OnClickListener { v: View? ->
            val ean = editTextEan.getText().toString()
            if (!ean.isEmpty()) {
                removeProduct(ean)
            } else {
                Toast.makeText(this, "Bitte EAN eingeben", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun removeProduct(ean: String?) {
        val baseUrl: String? = ApiConfig.BASE_URL
        val urlAddEan = baseUrl + "/removeProduct?ean=" + Uri.encode(ean)

        Thread(Runnable {
            try {
                val conn1 = URL(urlAddEan).openConnection() as HttpURLConnection
                conn1.setRequestMethod("DELETE")
                conn1.setDoOutput(false)
                conn1.connect()

                val responseCode = conn1.getResponseCode()
                conn1.disconnect()

                runOnUiThread(Runnable {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Produkt entfernt", Toast.LENGTH_SHORT).show()
                        editTextEan.setText("") // Feld leeren
                    } else {
                        Toast.makeText(this, "Produkt nicht gefunden", Toast.LENGTH_SHORT).show()
                    }
                })
            } catch (e: Exception) {
                runOnUiThread(Runnable {
                    Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                })
            }
        }).start()
    }
}