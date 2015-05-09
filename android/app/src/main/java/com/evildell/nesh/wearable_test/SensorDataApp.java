package com.evildell.nesh.wearable_test;

import android.app.Application;

import release.java.io.relayr.wearable.RelayrSdkInitializer;

/**
 * Created by nesh on 09.05.15.
 */
public class SensorDataApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RelayrSdkInitializer.initSdk(this);
    }
}
