package com.example.speedometer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.speedometer.views.CircularGaugeView

class MainActivity : AppCompatActivity() {
    private val list = listOf(10, 20, 30, 40, 50, 60, 70, 80, 120, 150, 100, 90)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    override fun onResume() {
        super.onResume()
        val view = findViewById<CircularGaugeView>(R.id.speedView)
        Thread {
            list.forEach {
                view.postDelayed({ view.setGaugeValue(it, 1000) }, 5000)
                //Thread.sleep(100)
            }
        }.run()
    }
}