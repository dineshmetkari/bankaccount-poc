package com.poc.android.bankaccount;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class App extends Application {
    private static final String TAG = "App";

    public static Context context = null;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        context = getApplicationContext();
    }
}
