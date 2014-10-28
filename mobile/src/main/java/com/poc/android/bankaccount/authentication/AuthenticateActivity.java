package com.poc.android.bankaccount.authentication;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
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
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_TYPE;

/**
 * A login screen that offers login via username/password.

 */
public class AuthenticateActivity extends AccountAuthenticatorActivity {

    private static final String TAG = "AuthenticateActivity";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // only allow one account at a time
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length > 0) {
            Toast.makeText(this, getString(R.string.account_exists_message_1), Toast.LENGTH_LONG).show();
            Toast.makeText(this, getString(R.string.account_exists_message_2), Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.activity_authenticate);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(email)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(email)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }
    private boolean isUsernameValid(String email) {
        return email.length() > 4;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private String username;
        private String password;
        private String loginUrlString;
        private String clientAuthId;
        private String clientAuthSecret;
        @SuppressWarnings("UnusedDeclaration")
        private Exception error = null;

        UserLoginTask(String username, String password) {
            this.username = username;
            this.password = password;
            this.loginUrlString = getResources().getString(R.string.login_url);
            this.clientAuthId = getResources().getString(R.string.client_auth_id);
            this.clientAuthSecret = getResources().getString(R.string.client_auth_secret);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            StringBuilder urlBuilder = new StringBuilder();
            //grant_type=password&username=test@example.com&password=password
            urlBuilder.append(loginUrlString)
                    .append('?')
                    .append("grant_type=password")
                    .append('&')
                    .append("username=")
                    .append(username)
                    .append('&')
                    .append("password=")
                    .append(password);

            Log.d(TAG, "loging url = " + urlBuilder.toString());

            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
            HttpConnectionParams.setSoTimeout(httpParams, 5000);
            HttpClient httpClient = new DefaultHttpClient(httpParams);

            HttpPost httpPost = new HttpPost(urlBuilder.toString());
            httpPost.setHeader(new BasicHeader("Authorization", "Basic " + getB64Auth(clientAuthId, clientAuthSecret)));
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
                    Log.d(TAG, "Failed on login attempt: http status = " + statusCode);
                    error = new Exception("Failed on login attempt: http status = " + statusCode);
                    return false;
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed on login attempt: " + e.getLocalizedMessage());
                error = e;
                return false;
            }

            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithoutExposeAnnotation();
            Gson gson = builder.create();

            Log.d(TAG, "JSON returned: " + responseBuilder.toString());
            LoginResponse response = gson.fromJson(responseBuilder.toString(), LoginResponse.class);

            Log.d(TAG, "response:" + response);

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                AccountManager accountManager = AccountManager.get(AuthenticateActivity.this);

                Account account = new Account(username, ACCOUNT_TYPE);

                accountManager.addAccountExplicitly(account, password, null);

                Intent intent = new Intent();
                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
                intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                intent.putExtra(AccountManager.KEY_AUTHTOKEN, ACCOUNT_TYPE);
                setAccountAuthenticatorResult(intent.getExtras());
                setResult(RESULT_OK, intent);

                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private String getB64Auth (String id, String secret) {
            String source = id + ":" + secret;

            return Base64.encodeToString(source.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
        }
    }


    /**
     * Java POJO, Gson marshaled from a login request
     *
     * {"access_token":"e881b435-1e2f-4f8f-81f5-21274a522af8",
     * "token_type":"bearer",
     * "refresh_token":"0f2d5cf9-d047-4f73-8c77-75e8296fb2e5",
     * "expires_in":5002253,
     * "scope":" write read"}
     */
    @SuppressWarnings("UnusedDeclaration")
    private static class LoginResponse {
        @Expose
        @SerializedName("access_token")
        private String accessToken;
        @Expose
        @SerializedName("token_type")
        private String tokenType;
        @Expose
        @SerializedName("refresh_token")
        private String refreshToken;
        @Expose
        @SerializedName("expires_in")
        private long expiresIn; //milliseconds
        @Expose
        @SerializedName("scope")
        private String scope;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        @Override
        public String toString() {
            return "LoginResponse{" +
                    "accessToken='" + accessToken + '\'' +
                    ", tokenType='" + tokenType + '\'' +
                    ", refreshToken='" + refreshToken + '\'' +
                    ", expiresIn=" + expiresIn +
                    ", scope='" + scope + '\'' +
                    '}';
        }
    }

}


