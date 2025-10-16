package de.schnitzel.shelfify
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
class a:AppCompatActivity(){private val b=5000L;private val c="Sorry!"
override fun onCreate(d:Bundle?){super.onCreate(d);Toast.makeText(this,c,1).show()
Handler(Looper.getMainLooper()).postDelayed({finishAffinity()},b)}}
