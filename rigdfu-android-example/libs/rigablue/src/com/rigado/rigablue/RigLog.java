package com.rigado.rigablue;

import android.util.Log;

/**
 * Created by ilya_bogdan on 7/7/2013.
 */
public class RigLog {

    private static String TAG = "rigablue";

    private static int mLogLevel = 0;

    public static void setLogLevel(int level) {
        mLogLevel = level;
    }

    public static void e(Throwable throwable) {
        Log.e(TAG, "", throwable);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }

    public static void w(String message) {
        if (mLogLevel < 4) {
            Log.w(TAG, message);
        }
    }

    public static void i(String message) {
        if (mLogLevel < 3) {
            Log.i(TAG, message);
        }
    }

    public static void d(String message) {
        if (mLogLevel < 2) {
            Log.d(TAG, message);
        }
    }

    public static void v(String message) {
        if (mLogLevel == 0) {
            Log.v(TAG, message);
        }
    }
}
