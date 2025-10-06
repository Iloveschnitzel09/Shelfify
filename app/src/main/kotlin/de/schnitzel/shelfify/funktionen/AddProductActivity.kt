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
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.funktionen.sub.BarcodeScannerActivity
import de.schnitzel.shelfify.prefs
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.Locale

class AddProductActivity : AppCompatActivity() {

    private lateinit var editTextEan: EditText
    private lateinit var editTextProductName: EditText
    private lateinit var editTextDate: EditText
    private var addEan = false

    private var barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ean = result.data?.getStringExtra("ean")
                editTextEan.setText(ean)
                checkEan(ean.toString())
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
//        val buttonCheckEan = findViewById<Button>(R.id.btnCheckEan)
        val buttonAddProduct = findViewById<Button>(R.id.btnAddProduct)

        editTextDate.setOnClickListener { openCustomDatePicker() }
        editTextEan.setOnClickListener {
            barcodeLauncher.launch(intent)
        }

        editTextEan.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
            barcodeLauncher.launch(intent)
        }

//        buttonCheckEan.setOnClickListener {
//            val ean = editTextEan.text.toString()
//            if (ean.isNotEmpty()) {
//                checkEan(ean)
//            } else {
//                Toast.makeText(this, "Bitte EAN eingeben", Toast.LENGTH_SHORT).show()
//            }
//        }

        buttonAddProduct.setOnClickListener {
            val ean = editTextEan.text.toString()
            val name = editTextProductName.text.toString()
            val datum = editTextDate.text.toString()

            if (ean.isEmpty() || datum.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Alle Felder ausfüllen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addNewProduct(ean, name, datum, addEan)
        }
    }

    private fun openCustomDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(20, 20, 20, 20)
        layout.gravity = Gravity.CENTER

        val yearPicker = NumberPicker(this)
        yearPicker.minValue = currentYear
        yearPicker.maxValue = currentYear + 20
        yearPicker.value = currentYear

        val monthPicker = NumberPicker(this)
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = currentMonth + 1

        // Day Picker
        val dayPicker = NumberPicker(this)
        dayPicker.minValue = 1
        dayPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        dayPicker.value = currentDay

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

        layout.addView(dayPicker)
        layout.addView(monthPicker)
        layout.addView(yearPicker)

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
        val token = prefs.getString("token", "null")
        val id = prefs.getInt("app_id", -1)
        val url = "${BASE_URL}/lookupProductName?ean=$ean&id=$id&token=$token"

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val name = response.body?.string() ?: ""

                    runOnUiThread {
                        editTextProductName.setText(name)
//                        editTextProductName.visibility = View.GONE
                        Toast.makeText(this, "Produktname gefunden", Toast.LENGTH_SHORT).show()
                        addEan = false
                    }
                } else {
                    runOnUiThread {
//                        editTextProductName.visibility = View.VISIBLE
                        Toast.makeText(
                            this,
                            "Produktname nicht gefunden – bitte eingeben",
                            Toast.LENGTH_SHORT
                        ).show()
                        addEan = true
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Fehler beim Abrufen", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun addNewProduct(ean: String, name: String, datum: String, add: Boolean) {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)
                val client = OkHttpClient()

                if (add) {
                    val eanFormBody = FormBody.Builder()
                        .add("ean", ean)
                        .add("name", name)
                        .add("id", id.toString())
                        .add("token", token ?: "")
                        .build()

                    val eanRequest = Request.Builder()
                        .url("$BASE_URL/addEAN")
                        .post(eanFormBody)
                        .build()

                    val response = client.newCall(eanRequest).execute()

                    if (!response.isSuccessful && response.code != 409) {
                        runOnUiThread {
                            Toast.makeText(this, "Fehler beim EAN-Hinzufügen", Toast.LENGTH_SHORT)
                                .show()
                        }
                        return@Thread
                    }
                    response.close()
                }

                val addProductBody = FormBody.Builder()
                    .add("name", name)
                    .add("ablaufdatum", datum)
                    .add("id", id.toString())
                    .add("token", token ?: "")
                    .build()

                val addProductRequest = Request.Builder()
                    .url("$BASE_URL/addProduct")
                    .post(addProductBody)
                    .build()

                val response = client.newCall(addProductRequest).execute()

                val responseCode = response.code
                response.close()

                runOnUiThread {
                    when (responseCode) {
                        200 -> {
                            Toast.makeText(this, "Produkt hinzugefügt", Toast.LENGTH_SHORT).show()
                            // Felder leeren
                            editTextEan.text.clear()
                            editTextProductName.text.clear()
                            editTextDate.text.clear()
//                            editTextProductName.visibility = View.GONE
                        }

                        409 -> Toast.makeText(
                            this,
                            "Produktname oder EAN existiert bereits",
                            Toast.LENGTH_SHORT
                        ).show()

                        else -> Toast.makeText(
                            this,
                            "Fehler beim Hinzufügen (Code: $responseCode)",
                            Toast.LENGTH_SHORT
                        ).show()
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