package com.example.safeetrip360

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SafetyTimerActivity : AppCompatActivity() {

    private lateinit var tvCountdown: TextView
    private lateinit var btnStartTimer: Button
    private lateinit var btnImSafe: Button
    private var countDownTimer: CountDownTimer? = null

    // DEMO TIME: 10 Seconds (for easy testing)
    private val START_TIME_IN_MILLIS: Long = 10000
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_timer)

        // Initialize Views
        tvCountdown = findViewById(R.id.tvCountdown)
        btnStartTimer = findViewById(R.id.btnStartTimer)
        btnImSafe = findViewById(R.id.btnImSafe)

        btnStartTimer.setOnClickListener {
            startTimer()
        }

        btnImSafe.setOnClickListener {
            stopTimer()
        }
    }

    private fun startTimer() {
        btnStartTimer.visibility = View.GONE
        btnImSafe.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(START_TIME_IN_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                tvCountdown.text = timeFormatted
            }

            override fun onFinish() {
                triggerSOSAlert()
            }
        }.start()

        isTimerRunning = true
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        tvCountdown.text = "SAFE"
        btnStartTimer.visibility = View.VISIBLE
        btnImSafe.visibility = View.GONE
        Toast.makeText(this, "Timer Cancelled. You are safe.", Toast.LENGTH_SHORT).show()
    }

    private fun triggerSOSAlert() {
        tvCountdown.text = "SOS SENT!"
        Toast.makeText(this, "SOS Alert Sent to Guardians!", Toast.LENGTH_LONG).show()
        // You can add your sendSMS() function here later
    }
}