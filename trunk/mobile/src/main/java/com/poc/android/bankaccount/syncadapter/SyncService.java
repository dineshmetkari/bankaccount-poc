package com.poc.android.bankaccount.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SyncService extends Service {
    private static final String TAG = "SyncService";

    private static SyncAdapter syncAdapter = null;
    private static final Object lock = new Object();

    @Override
    public void onCreate() {
        Log.d(TAG, "in onCreate()");
        synchronized (lock) {
            if (syncAdapter == null) {
                syncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    public SyncService() { }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
