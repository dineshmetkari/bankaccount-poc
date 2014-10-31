package com.poc.android.bankaccount;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.poc.android.bankaccount.contentprovider.AccountContentObserver;
import com.poc.android.bankaccount.contentprovider.AccountListener;
import com.poc.android.bankaccount.model.BankAccount;
import com.poc.android.bankaccount.syncadapter.SyncStatusObserverImpl;

import java.text.NumberFormat;

import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_TYPE;
import static com.poc.android.bankaccount.authentication.Authenticator.REFRESH_AUTH_TOKEN_TYPE;
import static com.poc.android.bankaccount.contentprovider.AccountContentProvider.ACCOUNT_ALL_FIELDS;
import static com.poc.android.bankaccount.contentprovider.AccountContentProvider.AUTHORITY;
import static com.poc.android.bankaccount.contentprovider.AccountContentProvider.ACCOUNTS_CONTENT_URI;


public class MainActivity extends FragmentActivity {
    @SuppressWarnings("FieldCanBeLocal")
    private static String TAG = "MainActivity";

    private AccountContentObserver accountContentObserver;
    private SyncStatusObserver syncStatusObserver;
    private Object syncStatusObserverHandle;
    private PlaceholderFragment fragment = new PlaceholderFragment();

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
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

        syncStatusObserver = new SyncStatusObserverImpl();

        accountContentObserver = new AccountContentObserver(null, fragment);

        getContentResolver().registerContentObserver(ACCOUNTS_CONTENT_URI, true, accountContentObserver);

        AccountManager accountManager = AccountManager.get(this);

        accountManager.addOnAccountsUpdatedListener(onAccountsUpdateListener, null, true);

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts.length > 0) {
            for (Account account : accounts) {
                Log.d(TAG, "account found: " + account);
            }

            accountManager.getAuthToken(accounts[0], REFRESH_AUTH_TOKEN_TYPE, null, this, getAuthTokenCallback, null);
        } else {
            Log.d(TAG, "no accounts found");

           accountManager.addAccount(ACCOUNT_TYPE, REFRESH_AUTH_TOKEN_TYPE, new String[] {}, null, this, addAccountCallback, null);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS;
        syncStatusObserverHandle = ContentResolver.addStatusChangeListener(mask, syncStatusObserver);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (syncStatusObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(syncStatusObserverHandle);
            syncStatusObserverHandle = null;
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AccountManager accountManager = AccountManager.get(this);
        accountManager.removeOnAccountsUpdatedListener(onAccountsUpdateListener);
        getContentResolver().unregisterContentObserver(accountContentObserver);
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

        fragment.hideAccount();

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

        fragment.hideAccount();

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

            accountManager.addAccount(ACCOUNT_TYPE, REFRESH_AUTH_TOKEN_TYPE, new String[]{}, null, this, callback, null);
        }
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
    public static class PlaceholderFragment extends Fragment implements AccountListener {
        private static final String TAG = "PlaceholderFragment";

        private TextView accountBalance;
        private TextView accountName;
        private View balanceLayout;
        private View accountNameLayout;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            //noinspection UnnecessaryLocalVariable
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            accountBalance = (TextView) rootView.findViewById(R.id.balanceTextView);
            accountName = (TextView) rootView.findViewById(R.id.accountTextView);
            balanceLayout = rootView.findViewById(R.id.balanceLayout);
            accountNameLayout = rootView.findViewById(R.id.accountNameLayout);

            return rootView;
        }

        @Override
        public void update() {
            Log.d(TAG, "update()");

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "on UI Thread");
                    Cursor cursor = getActivity().getContentResolver().query(ACCOUNTS_CONTENT_URI, ACCOUNT_ALL_FIELDS, null, new String[]{}, null);
                    BankAccount bankAccount = BankAccount.cursorToBankAccount(cursor);
                    cursor.close();

                    NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                    double amount = bankAccount.getBalance();
                    amount = amount / 100;

                    accountBalance.setText(numberFormat.format(amount));
                    accountName.setText(bankAccount.getName());

                    balanceLayout.setVisibility(View.VISIBLE);
                    accountNameLayout.setVisibility(View.VISIBLE);
                }
            });
        }

        public void hideAccount() {
            balanceLayout.setVisibility(View.INVISIBLE);
            accountNameLayout.setVisibility(View.INVISIBLE);
        }
    }
}
