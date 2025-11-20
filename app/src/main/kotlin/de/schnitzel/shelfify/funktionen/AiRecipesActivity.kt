package de.schnitzel.shelfify.funktionen

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.util.Recipe
import de.schnitzel.shelfify.util.adapter.RecipeAdapter

class AiRecipesActivity : AppCompatActivity() {

    val btnSearch: Button = findViewById(R.id.btnSearch)

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_show_all)
        btnSearch.isVisible = false

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = RecipeAdapter(createRecipe())
        recyclerView.adapter = adapter
    }


    //ONLY FOR TEST LATER RECEIVED FROM THE API
    fun createRecipe(): List<Recipe> {

        val r1 = Recipe(
            "Italienische Tomaten-Basilikum-Pasta",
            45,
            "500g Spaghetti, 800g frische Tomaten, 1 Bund Basilikum, 4 Knoblauchzehen, 100ml Olivenöl, Salz, Pfeffer, Parmesan",
            "1. Nudeln al dente kochen\n2. Tomaten blanchieren, häuten und würfeln\n3. Knoblauch in Olivenöl anbraten\n4. Tomaten zugeben und 20 Minuten köcheln\n5. Mit Basilikum, Salz und Pfeffer abschmecken\n6. Sauce mit Nudeln vermengen und mit Parmesan servieren"
        )

        val r2 = Recipe(
            "Hähnchen-Curry mit Basmatireis",
            60,
            "4 Hähnchenbrustfilets, 2 Zwiebeln, 3 Knoblauchzehen, 2 EL Currypulver, 400ml Kokosmilch, 200g Basmatireis, 2 Paprika, 1 Brokkoli, Salz, Öl",
            "1. Reis nach Packungsanweisung kochen\n2. Hähnchen in Würfel schneiden und anbraten\n3. Zwiebeln und Knoblauch dünsten\n4. Currypulver zugeben und kurz anrösten\n5. Kokosmilch angießen und 25 Minuten köcheln\n6. Gemüse in den letzten 10 Minuten zugeben\n7. Mit Salz abschmecken und mit Reis servieren"
        )

        val r3 = Recipe(
            "Schokoladen-Soufflé mit Vanilleeis",
            35,
            "150g Zartbitterschokolade, 4 Eier, 100g Zucker, 50g Butter, 30g Mehl, Puderzucker, Vanilleeis",
            "1. Schokolade im Wasserbad schmelzen\n2. Eiweiß zu steifem Schnee schlagen\n3. Eigelb mit Zucker schaumig rühren\n4. Geschmolzene Schokolade und Mehl unterheben\n5. Eischnee vorsichtig unterheben\n6. In gefettete Förmchen füllen\n7. Bei 180°C 12-15 Minuten backen\n8. Mit Puderzucker bestäuben und mit Vanilleeis servieren"
        )

        return listOf(r1, r2, r3)
    }
}