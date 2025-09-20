package de.schnitzel.shelfify.funktionen

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig
import de.schnitzel.shelfify.funktionen.sub.BarcodeScannerActivity
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.Locale

class AddProductActivity : AppCompatActivity() {

    private lateinit var editTextEan: EditText
    private lateinit var editTextProductName: EditText
    private lateinit var editTextDate: EditText
    private var baseUrl = ApiConfig.BASE_URL

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

        setContentView(R.layout.activity_addproduct)

        editTextEan = findViewById(R.id.etEan)
        editTextProductName = findViewById(R.id.etName)
        editTextDate = findViewById(R.id.etDate)
        val buttonCheckEan = findViewById<Button>(R.id.btnCheckEan)
        val buttonAddProduct = findViewById<Button>(R.id.btnAddProduct)

        editTextDate.setOnClickListener { openCustomDatePicker() }
        editTextEan.setOnClickListener {
            barcodeLauncher.launch(intent)
        }

        buttonCheckEan.setOnClickListener {
            val ean = editTextEan.text.toString()
            if (ean.isNotEmpty()) {
                checkEan(ean)
            } else {
                Toast.makeText(this, "Bitte EAN eingeben", Toast.LENGTH_SHORT).show()
            }
        }

        buttonAddProduct.setOnClickListener {
            val ean = editTextEan.text.toString()
            val name = editTextProductName.text.toString()
            val datum = editTextDate.text.toString()

            if (ean.isEmpty() || datum.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Alle Felder ausfüllen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addNewProduct(ean, name, datum)
        }
    }

    private fun openCustomDatePicker() {
        // Aktuelles Datum
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based!
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Layout mit horizontalem LinearLayout
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(20, 20, 20, 20)
        layout.gravity = Gravity.CENTER

        // Year Picker
        val yearPicker = NumberPicker(this)
        yearPicker.minValue = currentYear
        yearPicker.maxValue = currentYear + 20 // z.B. 20 Jahre in die Zukunft
        yearPicker.value = currentYear

        // Month Picker
        val monthPicker = NumberPicker(this)
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = currentMonth + 1

        // Day Picker
        val dayPicker = NumberPicker(this)
        dayPicker.minValue = 1
        dayPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        dayPicker.value = currentDay

        // Monat und Jahr ändern -> Tage aktualisieren
        val dateChangeListener = NumberPicker.OnValueChangeListener { picker, oldVal, newVal ->
            val year = yearPicker.value
            val month = monthPicker.value
            val tempCal = Calendar.getInstance()
            tempCal.set(Calendar.YEAR, year)
            tempCal.set(Calendar.MONTH, month - 1) // 0-based!
            val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            dayPicker.maxValue = maxDay
        }
        yearPicker.setOnValueChangedListener(dateChangeListener)
        monthPicker.setOnValueChangedListener(dateChangeListener)

        // Picker zur Ansicht hinzufügen
        layout.addView(dayPicker)
        layout.addView(monthPicker)
        layout.addView(yearPicker)

        // Dialog anzeigen
        AlertDialog.Builder(this)
            .setTitle("Ablaufdatum wählen")
            .setView(layout)
            .setPositiveButton("OK") { dialog, which ->
                val day = dayPicker.value
                val month = monthPicker.value
                val year = yearPicker.value

                val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
                editTextDate.setText(dateStr)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun checkEan(ean: String) {
        val url = "$baseUrl/lookupProductName?ean=$ean"

        Thread {
            try {
                val apiUrl = URL(url)
                val conn = apiUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val code = conn.responseCode
                if (code == 200) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(inputStream.reader())
                    val name = reader.readLine()
                    runOnUiThread {
                        editTextProductName.setText(name)
                        editTextProductName.visibility = View.GONE // weil gefunden
                        Toast.makeText(this, "Produktname gefunden", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        editTextProductName.visibility = View.VISIBLE
                        Toast.makeText(this, "Produktname nicht gefunden – bitte eingeben", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Fehler beim Abrufen", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun addNewProduct(ean: String, name: String, datum: String) {
        Thread {
            try {
                // 1. POST an /addEAN
                val url1 = URL("$baseUrl/addEAN")
                val conn1 = url1.openConnection() as HttpURLConnection
                conn1.requestMethod = "POST"
                conn1.doOutput = true
                conn1.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData1 = "ean=${URLEncoder.encode(ean, "UTF-8")}" +
                        "&name=${URLEncoder.encode(name, "UTF-8")}"

                conn1.outputStream.use { os ->
                    os.write(postData1.toByteArray(StandardCharsets.UTF_8))
                }
                conn1.responseCode
                conn1.disconnect()

                // 2. POST an /addProduct
                val url2 = URL("$baseUrl/addProduct")
                val conn2 = url2.openConnection() as HttpURLConnection
                conn2.requestMethod = "POST"
                conn2.doOutput = true
                conn2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData2 = "name=${URLEncoder.encode(name, "UTF-8")}" +
                        "&ablaufdatum=${URLEncoder.encode(datum, "UTF-8")}"

                conn2.outputStream.use { os ->
                    os.write(postData2.toByteArray(StandardCharsets.UTF_8))
                }

                val responseCode = conn2.responseCode
                conn2.disconnect()

                runOnUiThread {
                    when (responseCode) {
                        200 -> {
                            Toast.makeText(this, "Produkt hinzugefügt", Toast.LENGTH_SHORT).show()
                            // Felder leeren
                            editTextEan.text.clear()
                            editTextProductName.text.clear()
                            editTextDate.text.clear()
                            editTextProductName.visibility = View.GONE
                        }
                        409 -> Toast.makeText(this, "Produktname oder EAN existiert bereits", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this, "Fehler beim Hinzufügen (Code: $responseCode)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}