// IEngineValueListener.aidl
package com.example.speedometer;

interface IEngineValueListener {
      oneway void onSetSpeed(int speed);
      oneway void onSetEngineRate(int engineRate);
}