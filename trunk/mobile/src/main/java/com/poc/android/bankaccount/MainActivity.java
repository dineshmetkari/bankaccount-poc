package com.poc.android.bankaccount;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.poc.android.bankaccount.authentication.AuthenticateActivity;

import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_READ_AUTH_TOKEN_TYPE;
import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_TYPE;


public class MainActivity extends Activity implements AccountManagerCallback<Bundle> {
    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "in onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        AccountManager accountManager = AccountManager.get(this);

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts.length > 0) {
            for (Account account : accounts) {
                Log.d(TAG, "account found: " + account);
            }
        } else {
            Log.d(TAG, "no accounts found");
//            Account account = new Account("bar@example.com", Authenticator.ACCOUNT_TYPE);
            //noinspection UnusedDeclaration
            AccountManagerFuture<Bundle> bundleAccountManagerFuture = accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_READ_AUTH_TOKEN_TYPE, new String[] {}, null, this, this, null);

//            Bundle result = null;
//            try {
//                result = bundleAccountManagerFuture.getResult();
//            } catch (OperationCanceledException e) {
//                Log.e(TAG, "account creation error", e);
//            } catch (IOException e) {
//                Log.e(TAG, "account creation error", e);
//            } catch (AuthenticatorException e) {
//                Log.e(TAG, "account creation error", e);
//            }
//
//            Log.d(TAG, "addAccount() result: " + result);
        }

//        accountManager.getAuthToken(account, Authenticator.ACCOUNT_READ_AUTH_TOKEN_TYPE, null, this, this, null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void displayLoginForm(View view) {
        Intent loginIntent = new Intent(this, AuthenticateActivity.class);
        startActivity(loginIntent);
    }

    @Override
    public void run(AccountManagerFuture<Bundle> future) {
        Log.d(TAG, "in run()");
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            //noinspection UnnecessaryLocalVariable
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
}
