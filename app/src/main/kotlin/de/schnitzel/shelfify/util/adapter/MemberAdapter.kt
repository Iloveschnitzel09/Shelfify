package de.schnitzel.shelfify.util.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiClient
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

class MemberAdapter(
    private val members: MutableList<String>,
    private val isOwner: Boolean
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailText: TextView = view.findViewById(R.id.etInviteEmail)
        val removeButton: Button = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.member, parent, false)
        return MemberViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val email = members[position]
        holder.emailText.text = email
        
        val ownEmail = prefs.getString("email", "null")
        holder.removeButton.visibility =
            if (isOwner && !email.equals(ownEmail, ignoreCase = true)) View.VISIBLE else View.GONE

        holder.removeButton.setOnClickListener { kick(holder, email) }
    }

    private fun kick(holder: MemberViewHolder, email: String) {
        val context = holder.itemView.context
        val activity = context as? AppCompatActivity ?: return

        val accessToken = prefs.getString("accessToken", "")
        val request = Request.Builder()
            .url("$BASE_URL/kickMember")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(FormBody.Builder().add("email", email).build())
            .build()

        activity.lifecycleScope.launch {
            try {
                val code = withContext(Dispatchers.IO) {
                    ApiClient.client.newCall(request).execute().use { response -> response.code }
                }

                when (code) {
                    200 -> {
                        val pos = members.indexOf(email)
                        if (pos != -1) {
                            members.removeAt(pos)
                            notifyItemRemoved(pos)
                        }
                        toast(context, "$email wurde entfernt")
                    }
                    401 -> toast(context, "Ungültige Anmeldedaten")
                    403 -> toast(context, "Nur der Gruppenbesitzer kann Mitglieder entfernen")
                    404 -> toast(context, "Benutzer nicht gefunden")
                    502 -> toast(context, "Netzwerkfehler")
                    else -> toast(context, "Fehler beim Entfernen (Code: $code)")
                }
            } catch (e: Exception) {
                Log.e("MemberAdapter", "Fehler beim Entfernen", e)
                toast(context, "Netzwerkfehler")
            }
        }
    }

    private fun toast(context: android.content.Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount() = members.size
}