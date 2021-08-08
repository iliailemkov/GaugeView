package com.example.speedometer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.speedometer.views.CircularGaugeView

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val listener = object : IEngineValueListener.Stub() {
        override fun onSetSpeed(speed: Int) {
            Log.i("Speed", "onSetSpeed")
            handler.post { speedView?.setGaugeValue(speed, 100) }
        }

        override fun onSetEngineRate(engineRate: Int) {
            Log.i("Speed", "onSetEngineRate")
            handler.post { engineRateView?.setGaugeValue(engineRate, 0) }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i("Speed", "onServiceConnected")
            engineValueGenerator = IEngineValueGenerator.Stub.asInterface(service)
            engineValueGenerator?.registerListener(listener)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i("Speed", "onServiceDisconnected")
            engineValueGenerator?.unregisterListener()
            engineValueGenerator = null
        }

    }

    private var engineValueGenerator: IEngineValueGenerator? = null
    private var speedView: CircularGaugeView? = null
    private var engineRateView: CircularGaugeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        speedView = findViewById(R.id.speedView)
    }

    override fun onResume() {
        super.onResume()
        bindService(
            Intent(this, EngineValueGeneratorService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        unbindService(serviceConnection)
    }
}