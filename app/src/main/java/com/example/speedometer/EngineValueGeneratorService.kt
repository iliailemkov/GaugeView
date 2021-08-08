package com.example.speedometer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.lang.Math.abs
import java.util.*
import kotlin.math.sin

class EngineValueGeneratorService : Service() {

    private var engineListener: IEngineValueListener? = null

    private val engineValueGenerator = object : IEngineValueGenerator.Stub() {
        override fun registerListener(listener: IEngineValueListener) {
            Log.i("Speed", "registerListener")
            engineListener = listener
        }

        override fun unregisterListener() {
            Log.i("Speed", "unregisterListener")
            engineListener = null
        }
    }

    private var generatorThread : Thread? = null

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("TestService", "onBind")
        generatorThread = Thread(::generator)
        generatorThread?.start()
        return engineValueGenerator
    }

    override fun onUnbind(intent: Intent?): Boolean {
        generatorThread?.interrupt()
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("TestService", "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("TestService", "onDestroy")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun generator() {
        while (true) {
            Thread.sleep(100)
            engineListener?.onSetEngineRate(kotlin.math.abs(sin(Date().time.toDouble()) * 7000).toInt())
            engineListener?.onSetSpeed(kotlin.math.abs(sin(Date().time.toDouble()) * 220).toInt())
        }
    }
}