package com.poc.android.bankaccount.syncadapter;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.poc.android.bankaccount.authentication.Authenticator;

import java.io.IOException;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    @SuppressWarnings("UnusedDeclaration")
    private ContentResolver contentResolver;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        contentResolver = context.getContentResolver();
    }

    @SuppressWarnings("UnusedDeclaration")
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "in onPerformSync()");

        AccountManager accountManager = AccountManager.get(getContext());
        AccountManagerFuture<Bundle> result = accountManager.getAuthToken(account, Authenticator.TOKEN_TYPE, null, true, null, null);

        Bundle resultBundle;
        try {
            resultBundle = result.getResult();
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Sync operation cancelled:" + e.getMessage());
            return;
        } catch (IOException e) {
            Log.e(TAG, "Sync operation failed:" + e.getMessage());
            return;
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Authenticator failed:" + e.getMessage());
            return;
        }

        assert resultBundle != null;
        String authToken = resultBundle.getString(AccountManager.KEY_AUTHTOKEN);
        Log.d(TAG, "authToken = " + authToken);
    }
}
