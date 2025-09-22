package de.schnitzel.shelfify.util

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Button

fun disableButton(button: Button, tickText: String, finishText: String, seconds: Long) {
    Handler(Looper.getMainLooper()).post {
        button.isEnabled = false
        object : CountDownTimer(seconds * 1000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                button.text = "$tickText ${millisUntilFinished / 1000}s"
            }


            override fun onFinish() {
                button.isEnabled = true
                button.text = finishText
            }
        }.start()
    }
}