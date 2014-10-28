package com.poc.android.bankaccount;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_READ_AUTH_TOKEN_TYPE;
import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_TYPE;
import static com.poc.android.bankaccount.contentprovider.AccountContentProvider.AUTHORITY;


public class MainActivity extends FragmentActivity {
    @SuppressWarnings("FieldCanBeLocal")
    private static String TAG = "MainActivity";

    private AccountManagerCallback<Bundle> addAccountCallback = new AccountManagerCallback<Bundle>() {
        private String TAG = "addAccountCallback";
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            Log.d(TAG, "in run()");

            AccountManager accountManager = AccountManager.get(MainActivity.this);

            Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

            if (accounts.length > 0) {
                for (Account account : accounts) {
                    Log.d(TAG, "account found: " + account);
                }
            } else {
                Log.d(TAG, "no accounts found");
            }
        }
    };

    private AccountManagerCallback<Boolean> removeAccountCallback = new AccountManagerCallback<Boolean>() {
        private String TAG = "removeAccountCallback";
        @Override
        public void run(AccountManagerFuture<Boolean> future) {
            Log.d(TAG, "in run()");
        }
    };
    private AccountManagerCallback<Bundle> getAuthTokenCallback = new AccountManagerCallback<Bundle>() {
        private String TAG = "getAuthTokenCallback";
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            Log.d(TAG, "in run()");
        }
    };

    private OnAccountsUpdateListener onAccountsUpdateListener = new OnAccountsUpdateListener() {
        private String TAG = "onAccountsUpdateListener";
        @Override
        public void onAccountsUpdated(Account[] accounts) {
            Log.d(TAG, "in onAccountsUpdated()");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "in onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        AccountManager accountManager = AccountManager.get(this);

        accountManager.addOnAccountsUpdatedListener(onAccountsUpdateListener, null, true);

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts.length > 0) {
            for (Account account : accounts) {
                Log.d(TAG, "account found: " + account);
            }

            accountManager.getAuthToken(accounts[0], ACCOUNT_READ_AUTH_TOKEN_TYPE, null, this, getAuthTokenCallback, null);
        } else {
            Log.d(TAG, "no accounts found");

           accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_READ_AUTH_TOKEN_TYPE, new String[] {}, null, this, addAccountCallback, null);
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

    public void signOff(View view) {
        Log.d(TAG, "in signOff()");
        AccountManager accountManager = AccountManager.get(this);

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts.length > 0) {
            for (Account account : accounts) {
                Log.d(TAG, "account found: " + account);
                accountManager.removeAccount(account, removeAccountCallback, null);
            }
        } else {
            Log.d(TAG, "no accounts found");
        }
    }

    public void getBalance(View view) {
        Log.d(TAG, "in getBalance()");

        Account account = getAccount();

        final Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        if (account != null) {
            ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
        } else {
            AccountManager accountManager = AccountManager.get(this);

            AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                private String TAG = "addAccountCallback";
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    Log.d(TAG, "in run()");

                    Account newAccount = getAccount();
                    if (newAccount != null) {
                        ContentResolver.requestSync(newAccount, AUTHORITY, settingsBundle);
                    } else {
                        Toast.makeText(MainActivity.this, "error on sign in", Toast.LENGTH_LONG).show();
                    }
                }
            };

            accountManager.addAccount(ACCOUNT_TYPE, ACCOUNT_READ_AUTH_TOKEN_TYPE, new String[]{}, null, this, callback, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AccountManager accountManager = AccountManager.get(this);
        accountManager.removeOnAccountsUpdatedListener(onAccountsUpdateListener);
    }

    private Account getAccount() {
        AccountManager accountManager = AccountManager.get(MainActivity.this);

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts.length > 0) {
            for (Account account : accounts) {
                Log.d(TAG, "account found: " + account);
            }
            return accounts[0];
        } else {
            Log.d(TAG, "no accounts found");
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            //noinspection UnnecessaryLocalVariable
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }
}
