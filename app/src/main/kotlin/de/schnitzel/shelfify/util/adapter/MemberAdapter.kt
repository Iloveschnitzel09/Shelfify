package de.schnitzel.shelfify.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import de.schnitzel.shelfify.R
import de.schnitzel.shelfify.api.ApiConfig.BASE_URL
import de.schnitzel.shelfify.prefs
import java.net.HttpURLConnection
import java.net.URL

class MemberAdapter(
    private val members: List<String>,
    private val isOwner: Boolean
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailText: TextView = view.findViewById(R.id.etInviteEmail)
        val removeButton : Button = view.findViewById(R.id.btnRemove)
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


        holder.removeButton.visibility = if (isOwner && email != prefs.getString("email", "null")) View.VISIBLE else View.GONE

        holder.removeButton.setOnClickListener {
            Thread {
                val token = prefs.getString("token", "null")
                val id = prefs.getInt("app_id", -1)
                val conn = URL("${BASE_URL}/kickFromDatagroup?id=$id&token=$token&email=$email").openConnection() as HttpURLConnection


                conn.requestMethod = "POST"
                val code = conn.responseCode
                conn.disconnect()

                if (code == 200) {
                    (holder.itemView.context as Activity).runOnUiThread {
                        Toast.makeText(holder.itemView.context, "$email wurde entfernt", Toast.LENGTH_SHORT).show()
                        (members as MutableList).remove(email)
                        notifyItemRemoved(holder.absoluteAdapterPosition)

                    }
                } else {
                    (holder.itemView.context as Activity).runOnUiThread {
                        Toast.makeText(holder.itemView.context, "Fehler beim Entfernen", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    override fun getItemCount() = members.size
}
