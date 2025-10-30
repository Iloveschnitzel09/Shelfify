package de.schnitzel.shelfify.util.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.Products
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class ProductAdapter(private val productList: List<Products>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private val client = OkHttpClient()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_produkte, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = productList[position]

        holder.tvProduktname.text = item.produktname
        holder.tvMenge.text = "Menge: ${item.menge}"

        try {
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            val ablaufDate = apiFormat.parse(item.ablaufdatum)!!
            val ablaufAnzeigen = displayFormat.format(ablaufDate)
            holder.tvAblaufdatum.text = "Ablaufdatum: $ablaufAnzeigen"

            val heute = LocalDate.now()
            val ablauf = ablaufDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val daysBetween = ChronoUnit.DAYS.between(heute, ablauf)

            when {
                daysBetween < 0 -> {
                    holder.tvAblaufdatum.setTextColor(Color.RED)
                }

                daysBetween <= 3 -> {
                    holder.tvAblaufdatum.setTextColor("#FFA500".toColorInt())
                }

                else -> {
                    holder.tvAblaufdatum.setTextColor("#FF03DAC5".toColorInt())
                }
            }

            holder.sectionProduct.setOnClickListener {
                val ctx = holder.itemView.context
                val input = EditText(ctx).apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    setText(item.produktname)
                    setSelection(text.length)
                }

                val dialog = AlertDialog.Builder(ctx)
                    .setTitle("Produkt umbenennen")
                    .setView(input)
                    .setPositiveButton("Umbenennen") { _, _ ->
                        val newName = input.text.toString().trim()
                        if (newName.isNotEmpty() && newName != item.produktname) {
                            renameProduct(item.produktname, newName, holder)
                        }
                    }
                    .setNegativeButton("Abbrechen", null)
                    .create()

                dialog.show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            holder.tvAblaufdatum.text = "Ablauf: Unbekannt"
            holder.tvAblaufdatum.setTextColor(Color.GRAY)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProduktname: TextView = itemView.findViewById(R.id.tvProductname)
        val tvMenge: TextView = itemView.findViewById(R.id.tvTime)
        val tvAblaufdatum: TextView = itemView.findViewById(R.id.tvAblaufdatum)
        val sectionProduct: LinearLayout = itemView.findViewById(R.id.sectionProduct)
    }

    fun renameProduct(oldName: String, newName: String, holder: ViewHolder) {
        Thread {
            try {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)

                val renameFormBody = FormBody.Builder()
                    .add("oldName", oldName)
                    .add("newName", newName)
                    .add("id", id.toString())
                    .add("token", token ?: "")
                    .build()


                val renameRequest = Request.Builder()
                    .url("$BASE_URL/addProduct")
                    .post(renameFormBody)
                    .build()

                client.newCall(renameRequest).execute().use { response ->
                    if(response.code == 200) {
                        holder.tvProduktname.text = newName
                        Log.d("ProductAdapter", "Produkt erfolgreich umbenannt.")
                    } else {
                        Log.e("ProductAdapter", "Fehler beim Umbenennen des Produkts: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProductAdapter", e.stackTrace.toString())
            }
        }.start()
    }
}