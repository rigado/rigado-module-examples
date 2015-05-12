package com.rigado.androidbtle;

import android.app.Application;

import com.rigado.rigablue.RigCoreBluetooth;

/**
 * Initialize the application
 * NOTE: be sure to add this class to your manifest with android:name=".ApplicationMain"
 */
public class ApplicationMain extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        // Required initialization
        RigCoreBluetooth.getInstance().setContext(this);
        RigCoreBluetooth.getInstance().init(getPackageName());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        RigCoreBluetooth.getInstance().finish();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        RigCoreBluetooth.getInstance().finish();
    }
}
