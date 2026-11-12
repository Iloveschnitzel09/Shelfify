package de.schnitzel.shelfify.funktionen

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiClient
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.funktionen.sub.BarcodeScannerActivity
import de.schnitzel.shelfify.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.Locale

class AddProductActivity : AppCompatActivity() {

    private lateinit var editTextEan: EditText
    private lateinit var btnCheckEan: Button
    private lateinit var editTextProductName: EditText
    private lateinit var editTextDate: EditText
    private lateinit var etQuantity: EditText
    private lateinit var buttonAddProduct: Button

    private var addEan = false
    private val client = ApiClient.client

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ean = result.data?.getStringExtra("ean")

                if (!ean.isNullOrEmpty()) {
                    editTextEan.setText(ean)
                    checkEan(ean)
                }
            } else {
                editTextEan.requestFocus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addproduct)

        initViews()
        setupListeners()

        if (savedInstanceState == null) {
            startScannerOrFocus()
        }
    }

    private fun initViews() {
        editTextEan = findViewById(R.id.etEan)
        btnCheckEan = findViewById(R.id.btnCheckEan)
        editTextProductName = findViewById(R.id.etName)
        editTextDate = findViewById(R.id.etDate)
        etQuantity = findViewById(R.id.etQuantity)
        buttonAddProduct = findViewById(R.id.btnAddProduct)

        editTextProductName.isVisible = false
    }

    private fun setupListeners() {
        editTextDate.setOnClickListener { openCustomDatePicker() }

        btnCheckEan.setOnClickListener {
            val ean = editTextEan.text.toString().trim()
            if (ean.isNotEmpty()) {
                checkEan(ean)
            } else {
                Toast.makeText(this, "Bitte EAN eingeben", Toast.LENGTH_SHORT).show()
            }
        }

        buttonAddProduct.setOnClickListener {
            val ean = editTextEan.text.toString().trim()
            val name = editTextProductName.text.toString().trim()
            val datum = editTextDate.text.toString().trim()
            var quantity = etQuantity.text.toString().trim()

            if (quantity.isEmpty()) quantity = "1"

            if (ean.isEmpty() || datum.isEmpty() || (editTextProductName.isVisible && name.isEmpty())) {
                Toast.makeText(this, "Alle Felder ausfüllen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addNewProduct(ean, name, datum, addEan, quantity)
        }
    }

    private fun openScanner() {
        val intent = Intent(this, BarcodeScannerActivity::class.java)
        barcodeLauncher.launch(intent)
    }

    private fun resetAndRestart() {
        editTextEan.text.clear()
        editTextProductName.text.clear()
        editTextDate.text.clear()
        etQuantity.setText("1")

        editTextProductName.isVisible = false

        startScannerOrFocus()
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
        yearPicker.minValue = currentYear - 10
        yearPicker.maxValue = currentYear + 20
        yearPicker.value = currentYear

        val monthPicker = NumberPicker(this)
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = currentMonth + 1

        val dayPicker = NumberPicker(this)
        dayPicker.minValue = 1
        dayPicker.maxValue = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        dayPicker.value = currentDay

        val dateChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            val year = yearPicker.value
            val month = monthPicker.value

            val tempCal = Calendar.getInstance()
            tempCal.set(Calendar.YEAR, year)
            tempCal.set(Calendar.MONTH, month - 1)
            tempCal.set(Calendar.DAY_OF_MONTH, 1)

            val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            if (dayPicker.value > maxDay) {
                dayPicker.value = maxDay
            }
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
            .setPositiveButton("OK") { _, _ ->
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
        val accessToken = prefs.getString("accessToken", "")
        val url = "$BASE_URL/lookupProductName?ean=$ean"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        lifecycleScope.launch {
            try {
                val (isSuccessful, code, bodyString) = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        Triple(response.isSuccessful, response.code, response.body?.string() ?: "")
                    }
                }

                editTextProductName.isVisible = true

                if (isSuccessful) {
                    editTextProductName.setText(bodyString)
                    editTextProductName.isEnabled = false
                    addEan = false
                    Toast.makeText(this@AddProductActivity, "Produktname gefunden", Toast.LENGTH_SHORT).show()

                } else {
                    editTextProductName.text.clear()
                    editTextProductName.isEnabled = true
                    addEan = true

                    when (code) {
                        404 -> Toast.makeText(this@AddProductActivity, "Produktname nicht gefunden – bitte eintragen", Toast.LENGTH_SHORT).show()
                        502 -> Toast.makeText(this@AddProductActivity, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this@AddProductActivity, "Fehler: $code", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AddProduct", "Fehler beim Abrufen der EAN", e)
                Toast.makeText(this@AddProductActivity, "Fehler beim Abrufen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addNewProduct(ean: String, name: String, datum: String, add: Boolean, quantity: String) {
        val accessToken = prefs.getString("accessToken", "")
        lifecycleScope.launch {
            try {
                if (add) {
                    val eanFormBody = FormBody.Builder()
                        .add("ean", ean)
                        .add("productName", name)
                        .build()

                    val eanRequest = Request.Builder()
                        .url("$BASE_URL/addEan")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .post(eanFormBody)
                        .build()

                    val (isSuccessful, eanCode) = withContext(Dispatchers.IO) {
                        client.newCall(eanRequest).execute().use { response ->
                            Pair(response.isSuccessful, response.code)
                        }
                    }

                    if (!isSuccessful) {
                        when (eanCode) {
                            400 -> Toast.makeText(this@AddProductActivity, "Fehler beim EAN-Hinzufügen", Toast.LENGTH_SHORT).show()
                            409 -> Toast.makeText(this@AddProductActivity, "Name schon vorhanden", Toast.LENGTH_SHORT).show()
                            502 -> Toast.makeText(this@AddProductActivity, "Netzwerkfehler", Toast.LENGTH_SHORT).show()
                            else -> {
                                Toast.makeText(this@AddProductActivity, "Unerwarteter EAN Fehler: $eanCode", Toast.LENGTH_SHORT).show()
                                Log.e("AddProduct", "Unerwarteter Fehler: $eanCode$accessToken")
                            }
                        }
                        return@launch
                    }
                }

                val addProductBody = FormBody.Builder()
                    .add("ean", ean)
                    .add("ablaufdatum", datum)
                    .add("quantity", quantity)
                    .build()

                val addProductRequest = Request.Builder()
                    .url("$BASE_URL/addProduct")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(addProductBody)
                    .build()

                val responseCode = withContext(Dispatchers.IO) {
                    client.newCall(addProductRequest).execute().use { response ->
                        response.code
                    }
                }

                when (responseCode) {
                    200 -> {
                        Toast.makeText(this@AddProductActivity, "Produkt hinzugefügt", Toast.LENGTH_SHORT).show()
                        resetAndRestart()
                    }
                    409 -> Toast.makeText(this@AddProductActivity, "Produktname oder EAN existiert bereits", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this@AddProductActivity, "Fehler beim Hinzufügen (Code: $responseCode)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AddProduct", "Fehler beim Hinzufügen des Produkts", e)
                Toast.makeText(this@AddProductActivity, "Fehler beim Hinzufügen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScannerOrFocus() {
        if (prefs.getBoolean("start_with_scanner", true)) {
            openScanner()
        } else {
            editTextEan.requestFocus()
        }
    }
}