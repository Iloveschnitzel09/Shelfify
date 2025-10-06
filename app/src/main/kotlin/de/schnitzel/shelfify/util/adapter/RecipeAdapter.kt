package de.schnitzel.shelfify.util.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.util.Recipe

class RecipeAdapter(private val recipeList: List<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ai_recipes, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.tvRecipeName.text = recipe.name
        holder.tvTime.text = "${recipe.time} Minuten"
        holder.tvIngredients.text = recipe.ingredients
            .split(",")
            .joinToString("\n") { "• ${it.trim()}" }
        holder.tvProcessing.text = recipe.processing

    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipiename)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvIngredients: TextView = itemView.findViewById(R.id.tvIngredients)
        val tvProcessing: TextView = itemView.findViewById(R.id.tvProcessing)

    }
}