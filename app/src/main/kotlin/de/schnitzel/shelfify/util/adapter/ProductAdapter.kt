package de.schnitzel.shelfify.util.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.prefs
import de.schnitzel.shelfify.util.Products
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = productList[position]

        holder.tvProduktname.text = item.name
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
                    setText(item.name)
                    setSelection(text.length)
                }

                val dialog = AlertDialog.Builder(ctx)
                    .setTitle("Produkt umbenennen")
                    .setView(input)
                    .setPositiveButton("Umbenennen") { _, _ ->
                        val newName = input.text.toString().trim()
                        if (newName.isNotEmpty() && newName != item.name) {
                            renameProduct(item.name, newName, holder)
                        }
                    }
                    .setNegativeButton("Abbrechen", null)
                    .create()

                dialog.show()
            }

        } catch (e: Exception) {
            Log.e("ProductAdapter", e.message.toString())
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

    private fun renameProduct(oldName: String, newName: String, holder: ViewHolder) {
        val ctx = holder.itemView.context
        // Die Activity ist der LifecycleOwner, der Request wird beim Schließen abgebrochen
        val scope = (ctx as? LifecycleOwner)?.lifecycleScope ?: return
        val accessToken = prefs.getString("accessToken", "")

        val renameFormBody = FormBody.Builder()
            .add("oldName", oldName)
            .add("newName", newName)
            .build()

        val renameRequest = Request.Builder()
            .url("$BASE_URL/renameProduct")
            .addHeader("Authorization", "Bearer $accessToken")
            .put(renameFormBody)
            .build()

        scope.launch {
            try {
                val responseCode = withContext(Dispatchers.IO) {
                    client.newCall(renameRequest).execute().use { response -> response.code }
                }

                when (responseCode) {
                    200 -> {
                        Toast.makeText(ctx, "Produkt erfolgreich umbenannt", Toast.LENGTH_SHORT).show()
                        holder.tvProduktname.text = newName
                    }

                    409 -> Toast.makeText(ctx, "Produktname oder EAN existiert bereits", Toast.LENGTH_SHORT).show()

                    else -> Toast.makeText(ctx, "Fehler beim Umbenennen: $responseCode", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProductAdapter", "Fehler beim Umbenennen", e)
                Toast.makeText(ctx, "Fehler beim Umbenennen", Toast.LENGTH_SHORT).show()
            }
        }
    }
}