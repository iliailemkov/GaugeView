// IEngineValueGenerator.aidl
package com.example.speedometer;

import com.example.speedometer.IEngineValueListener;

interface IEngineValueGenerator {
    void registerListener(IEngineValueListener listener);
    void unregisterListener();
}