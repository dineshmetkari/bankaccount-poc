package com.poc.android.bankaccount.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.NetworkErrorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poc.android.bankaccount.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
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
import java.util.Arrays;

import static android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_INTENT;
import static android.accounts.AccountManager.get;
import static com.poc.android.bankaccount.syncadapter.SyncAdapter.SYNC_REQUEST_SOURCE_EXTRA;
import static com.poc.android.bankaccount.syncadapter.SyncAdapter.SYNC_REQUEST_SOURCE_HANDHELD;
import static com.poc.android.bankaccount.syncadapter.SyncAdapter.SYNC_REQUEST_SOURCE_UNSPECIFIED;
import static com.poc.android.bankaccount.syncadapter.SyncAdapter.SYNC_REQUEST_SOURCE_WEARABLE;

public class Authenticator extends AbstractAccountAuthenticator {
    private static final String TAG = "Authenticator";
    public static final String ACCESS_AUTH_TOKEN_TYPE = "access";
    public static final String REFRESH_AUTH_TOKEN_TYPE = "refresh";
    public static final String ACCOUNT_TYPE = "com.poc.android.bankaccount";
    public static final String AUTH_FAILED_ACTION = "auth_failed_action";
    public static final String ACCOUNT_NAME_EXTRA = "account_name_extra";

    private Context context;

    public Authenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.d(TAG, "editProperties(" + response + ", " + accountType + ")");
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "addAccount(" + response + ", " + accountType + ", " + authTokenType + ", " + Arrays.toString(requiredFeatures) + ", " + options + ")");
        Bundle result;
        Intent intent;

        intent = new Intent(context, AuthenticateActivity.class);
        intent.putExtra(ACCESS_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        result = new Bundle();
        result.putParcelable(KEY_INTENT, intent);

        return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "confirmCredentials(" + response + ", " + account + ", " +  options + ")");
        return null;
    }

    /**
     * getting here means the AccountManager auth token was null.  Most likely set to null
     * by someone who had access rejected while using it. So here we will prompt the user to password
     * which will result in a new auth token
     *
     * @param response from AccountManager
     * @param account from AccountManager
     * @param authTokenType from AccountManager
     * @param options from AccountManager
     * @return response {@link android.os.Bundle}
     * @throws NetworkErrorException
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "getAuthToken(" + response + ", " + account + ", " + authTokenType + ", " + options + ")");

        switch (options.getInt(SYNC_REQUEST_SOURCE_EXTRA)) {
            case (SYNC_REQUEST_SOURCE_HANDHELD):
                Log.d(TAG, "getAuthToken from handheld");
                break;
            case (SYNC_REQUEST_SOURCE_WEARABLE):
                Log.d(TAG, "getAuthToken from wearable");
                break;
            case (SYNC_REQUEST_SOURCE_UNSPECIFIED):
                Log.d(TAG, "getAuthToken from unspecified");
                break;
            default:
                Log.d(TAG, "getAuthToken from unknown source: "  + options.getInt(SYNC_REQUEST_SOURCE_EXTRA));
        }

        Intent intent = new Intent(context, AuthenticateActivity.class);
        intent.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        Bundle result = new Bundle();
        result.putParcelable(KEY_INTENT, intent);
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.d(TAG, "getAuthTokenLabel(" + authTokenType + ")");
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "updateCredentials(" + response + ", " + account + ", " + authTokenType + ", "  + options + ")");
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Log.d(TAG, "hasFeatures(" + response + ", " + account + ", " + Arrays.toString(features) + ")");
        return null;
    }

    public static String getB64Auth (String id, String secret) {
        String source = id + ":" + secret;

        return Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    @SuppressWarnings("UnusedDeclaration")
    private String getRefreshTokenFromCache(Account account) {
        Log.d(TAG, "fetching refresh token");

        AccountManager accountManager = get(context);
        AccountManagerFuture<Bundle> result = accountManager.getAuthToken(account, REFRESH_AUTH_TOKEN_TYPE, null, true, null, null);

        Bundle resultBundle;
        try {
            resultBundle = result.getResult();
        } catch (OperationCanceledException e) {
            Log.e(TAG, "Sync operation cancelled:" + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Sync operation failed:" + e.getMessage());
            return null;
        } catch (AuthenticatorException e) {
            Log.e(TAG, "Authenticator failed:" + e.getMessage());
            return null;
        }

        assert resultBundle != null;
        String refreshToken = resultBundle.getString(KEY_AUTHTOKEN);
        Log.d(TAG, "refreshToken = " + refreshToken);
        return refreshToken;
    }

    @SuppressWarnings("UnusedDeclaration")
    private AuthResponse refreshAccessToken(String refreshToken) {
        String loginUrlString = context.getResources().getString(R.string.login_url);
        String clientAuthId = context.getResources().getString(R.string.client_auth_id);
        String clientAuthSecret = context.getResources().getString(R.string.client_auth_secret);

        StringBuilder urlBuilder = new StringBuilder();
        //?grant_type=refresh_token&refresh_token=[your refresh token]
        urlBuilder.append(loginUrlString)
                .append('?')
                .append("grant_type=refresh_token")
                .append('&')
                .append("refresh_token=")
                .append(refreshToken);

        Log.d(TAG, "refresh token url = " + urlBuilder.toString());

        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        HttpClient httpClient = new DefaultHttpClient(httpParams);

        HttpPost httpPost = new HttpPost(urlBuilder.toString());

        BasicHeader authHeader = new BasicHeader("Authorization", "Basic " + Authenticator.getB64Auth(clientAuthId, clientAuthSecret));
        Log.d(TAG, "authHeader = " + authHeader);
        httpPost.setHeader(authHeader);
        httpPost.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        StringBuilder responseBuilder = new StringBuilder();

        try {
            HttpResponse response = httpClient.execute(httpPost);
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
                AuthResponse error = new AuthResponse();
                error.setError(new Exception("Failed on refresh access token: http status = " + statusCode));
                return error;
            }
        } catch (Exception e) {
            AuthResponse error = new AuthResponse();
            error.setError(e);
            return error;
        }

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();

        //noinspection UnnecessaryLocalVariable
        AuthResponse response = gson.fromJson(responseBuilder.toString(), AuthResponse.class);

        return response;
    }
}
