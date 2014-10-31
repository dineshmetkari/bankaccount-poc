package com.poc.android.bankaccount.syncadapter;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poc.android.bankaccount.R;
import com.poc.android.bankaccount.authentication.Authenticator;
import com.poc.android.bankaccount.contentprovider.AccountContentProvider;
import com.poc.android.bankaccount.model.BankAccount;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.poc.android.bankaccount.authentication.Authenticator.ACCESS_AUTH_TOKEN_TYPE;

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

        String authToken;

        try {
            authToken = accountManager.blockingGetAuthToken(account, ACCESS_AUTH_TOKEN_TYPE, true);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Sync operation cancelled:" + e.getMessage());
            return;
        } catch (IOException e) {
            Log.e(TAG, "Sync operation failed:" + e.getMessage());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Authenticator failed:" + e.getMessage());
            syncResult.stats.numAuthExceptions++;
            return;
        }

        assert authority != null;
        Log.d(TAG, "authToken = " + authToken);

        BankAccount bankAccount;

        try {
            bankAccount = getBankAccount(authToken);
        } catch (IOException e) {
            Log.e(TAG, "Sync operation failed:" + e.getMessage());
            syncResult.stats.numIoExceptions++;
            return;
        }

        Log.d(TAG, "bankAccount = " + bankAccount);

        ContentValues values = new ContentValues();
        values.put("id", bankAccount.getId());
        values.put("name", bankAccount.getName());
        values.put("balance", bankAccount.getBalance());

        try {
            int numRowsUpdated = provider.update(AccountContentProvider.ACCOUNTS_CONTENT_URI, values, null, new String[0]);
            Log.d(TAG, numRowsUpdated + " rows updated");
        } catch (RemoteException e) {
            Log.e(TAG, "Sync operation failed:" + e.getMessage());
            syncResult.stats.numIoExceptions++;
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    private BankAccount getBankAccount(String authToken) throws IOException {
        String loginUrlString = getContext().getResources().getString(R.string.account_url);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(loginUrlString)
                .append('/')
                .append("12345");

        Log.d(TAG, "account url = " + urlBuilder.toString());

        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        HttpClient httpClient = new DefaultHttpClient(httpParams);

        HttpGet httpGet = new HttpGet(urlBuilder.toString());

        BasicHeader authHeader = new BasicHeader("Authorization", "Bearer " + authToken);
        Log.d(TAG, "authHeader = " + authHeader);
        httpGet.setHeader(authHeader);
        httpGet.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        StringBuilder responseBuilder = new StringBuilder();

        HttpResponse response = httpClient.execute(httpGet);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            inputStream.close();
        } else {
            if (statusCode == 401) { // Unauthorized
                AccountManager accountManager = AccountManager.get(getContext());
                accountManager.invalidateAuthToken(Authenticator.ACCOUNT_TYPE, authToken);
            }

            throw new IOException("Failed on accessing account: http status = " + statusCode);
        }

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        Log.d(TAG, "response: " + responseBuilder.toString());

        //noinspection UnnecessaryLocalVariable
        BankAccount bankAccount = gson.fromJson(responseBuilder.toString(), BankAccount.class);

        return bankAccount;
    }
}
