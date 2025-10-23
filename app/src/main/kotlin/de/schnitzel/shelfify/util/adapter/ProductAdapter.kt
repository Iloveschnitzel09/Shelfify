package de.schnitzel.shelfify.util.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.util.Products
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class ProductAdapter(private val productList: List<Products>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

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
                            //renameProduct(item.produktname, newName) TODO: Implement renaming logic
                            holder.tvProduktname.text = newName
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
}