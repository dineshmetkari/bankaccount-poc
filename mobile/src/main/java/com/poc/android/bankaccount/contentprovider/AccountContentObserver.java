package com.poc.android.bankaccount.contentprovider;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class AccountContentObserver extends ContentObserver {
    private static final String TAG = "AccountContentObserver";

    private AccountListener accountListener;

    public AccountContentObserver(Handler handler, AccountListener accountListener) {
        super(handler);
        this.accountListener = accountListener;
    }

    @Override
    public boolean deliverSelfNotifications() {
        Log.d(TAG, "deliverSelfNotifications()");
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        Log.d(TAG, "onChange(" + selfChange + ")");
        super.onChange(selfChange);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(TAG, "onChange(" + selfChange + ", " + uri + ")");
        super.onChange(selfChange, uri);
        accountListener.update();
    }
}
