package com.poc.android.bankaccount;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poc.android.bankaccount.contentprovider.AccountContentObserver;
import com.poc.android.bankaccount.contentprovider.AccountListener;
import com.poc.android.bankaccount.library.model.BankAccount;
import com.poc.android.bankaccount.syncadapter.SyncStatusObserverImpl;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.poc.android.bankaccount.authentication.Authenticator.ACCESS_AUTH_TOKEN_TYPE;
import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_NAME_EXTRA;
import static com.poc.android.bankaccount.authentication.Authenticator.ACCOUNT_TYPE;
import static com.poc.android.bankaccount.authentication.Authenticator.AUTH_FAILED_ACTION;
import static com.poc.android.bankaccount.library.contentprovider.AccountContentProvider.ACCOUNTS_CONTENT_URI;
import static com.poc.android.bankaccount.library.contentprovider.AccountContentProvider.ACCOUNT_ALL_FIELDS;
import static com.poc.android.bankaccount.library.contentprovider.AccountContentProvider.AUTHORITY;


public class MainActivity extends FragmentActivity implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    @SuppressWarnings("FieldCanBeLocal")
    private static final String TAG = "MainActivity";

    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    public static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String GET_BALANCE_PATH = "/get-account-balance";
    private static final String AUTH_REQUIRED_PATH = "/auth-required";

    private AccountContentObserver accountContentObserver;
    private AccountContentObserver wearableAccountObserver;
    private SyncStatusObserver syncStatusObserver;
    private Object syncStatusObserverHandle;
    private PlaceholderFragment fragment = new PlaceholderFragment();

    private GoogleApiClient googleApiClient;
    private boolean resolvingError;

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

    private BroadcastReceiver authFailedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(ACCOUNT_NAME_EXTRA);
            Toast.makeText(MainActivity.this, "authentication failed for " + accountName, Toast.LENGTH_LONG).show();
            Account account = new Account(accountName, ACCOUNT_TYPE);

            AccountManager accountManager = AccountManager.get(MainActivity.this);

            accountManager.getAuthToken(account, ACCESS_AUTH_TOKEN_TYPE, null, MainActivity.this, getAuthTokenCallback, null);
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
        wearableAccountObserver = new AccountContentObserver(null, new WearableAccountListener());

        getContentResolver().registerContentObserver(ACCOUNTS_CONTENT_URI, true, accountContentObserver);
        getContentResolver().registerContentObserver(ACCOUNTS_CONTENT_URI, true, wearableAccountObserver);

        AccountManager accountManager = AccountManager.get(this);

        accountManager.addOnAccountsUpdatedListener(onAccountsUpdateListener, null, true);

        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts.length > 0) {
            for (Account account : accounts) {
                Log.d(TAG, "account found: " + account);
            }

            accountManager.getAuthToken(accounts[0], ACCESS_AUTH_TOKEN_TYPE, null, this, getAuthTokenCallback, null);
        } else {
            Log.d(TAG, "no accounts found");

           accountManager.addAccount(ACCOUNT_TYPE, ACCESS_AUTH_TOKEN_TYPE, new String[] {}, null, this, addAccountCallback, null);
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (! resolvingError) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS;
        syncStatusObserverHandle = ContentResolver.addStatusChangeListener(mask, syncStatusObserver);

        registerReceiver(authFailedBroadcastReceiver, new IntentFilter(AUTH_FAILED_ACTION));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        if (syncStatusObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(syncStatusObserverHandle);
            syncStatusObserverHandle = null;
        }

        unregisterReceiver(authFailedBroadcastReceiver);

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        if (! resolvingError) {
            Wearable.DataApi.removeListener(googleApiClient, this);
            Wearable.MessageApi.removeListener(googleApiClient, this);
            Wearable.NodeApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AccountManager accountManager = AccountManager.get(this);
        accountManager.removeOnAccountsUpdatedListener(onAccountsUpdateListener);
        getContentResolver().unregisterContentObserver(accountContentObserver);
        getContentResolver().unregisterContentObserver(wearableAccountObserver);
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void getBalanceButton(View view) {
        Log.d(TAG, "in getBalanceButton()");

        fragment.hideAccount();

        final Account account = getAccount();

        final Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        final AccountManager accountManager = AccountManager.get(this);

        if (account != null) {
            // if account found, check for cached auth token
            if (accountManager.peekAuthToken(account, ACCESS_AUTH_TOKEN_TYPE) == null) {
                // if no auth token, request a new one, this should result in sign on dialog
                AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
                    private String TAG = "getAuthTokenCallback";

                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Log.d(TAG, "in run()");

                        // after sign on dialog, check auth token again,
                        if (accountManager.peekAuthToken(account, ACCESS_AUTH_TOKEN_TYPE) != null) {
                            // auth token exists, request sync
                            ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
                        } else {
                            // otherwise some error or a cancelled sign on
                            Toast.makeText(MainActivity.this, "error on sign in", Toast.LENGTH_LONG).show();
                        }
                    }
                };

                accountManager.getAuthToken(account, ACCESS_AUTH_TOKEN_TYPE, null, this, callback, null);
            } else {
                // account and auth token exist, request sync
                ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
            }
        } else {
            // account not found, call addAccount which should present the sign on
            // activity and add an account and auth token
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

            accountManager.addAccount(ACCOUNT_TYPE, ACCESS_AUTH_TOKEN_TYPE, new String[]{}, null, this, callback, null);
        }
    }

    private void getBalanceWearable() {
        Log.d(TAG, "getBalanceWearable()");

        AccountManager accountManager = AccountManager.get(this);

        Account account = getAccount();

        if (account == null || accountManager.peekAuthToken(account, ACCESS_AUTH_TOKEN_TYPE) == null) {
            new SendAuthRequiredMessageTask().execute();
        } else {
            Bundle settingsBundle = new Bundle();
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

            ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
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

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(" + bundle + ")");

        resolvingError = false;
        Wearable.DataApi.addListener(googleApiClient, this);
        Wearable.MessageApi.addListener(googleApiClient, this);
        Wearable.NodeApi.addListener(googleApiClient, this);

        PendingResult<NodeApi.GetLocalNodeResult> getLocalNodeResultPendingResult = Wearable.NodeApi.getLocalNode(googleApiClient);

        getLocalNodeResultPendingResult.setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                Toast.makeText(MainActivity.this, "local node display name: " + getLocalNodeResult.getNode().getDisplayName(), Toast.LENGTH_LONG).show();
            }
        });

        PendingResult<NodeApi.GetConnectedNodesResult> getConnectedNodesResultPendingResult = Wearable.NodeApi.getConnectedNodes(googleApiClient);

        getConnectedNodesResultPendingResult.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                int numberOfNodes = 0;
                for (Node node : getConnectedNodesResult.getNodes()) {
                    numberOfNodes++;
                    Log.d(TAG, "connected node: " + node.getDisplayName());
                }

                if (numberOfNodes == 0) {
                    Log.d(TAG, "no nodes connected");
                    Toast.makeText(MainActivity.this, "no nodes connected", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(" + cause + ")");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(" + dataEvents + ")");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived(" + messageEvent + ")");
        if (messageEvent.getPath().equals(GET_BALANCE_PATH)) {
            Log.d(TAG, "incoming get balance request");
            getBalanceWearable();
        }
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "onPeerConnected(" + node + ")");
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "onPeerDisconnected(" + node + ")");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(" + connectionResult + ")");
        if (resolvingError) {
            // Already attempting to resolve an error.
            //noinspection UnnecessaryReturnStatement
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                resolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                googleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            resolvingError = false;
            Wearable.DataApi.removeListener(googleApiClient, this);
            Wearable.MessageApi.removeListener(googleApiClient, this);
            Wearable.NodeApi.removeListener(googleApiClient, this);
        }

    }

    public void startWearableActivity(View view) {
        Log.d(TAG, "startWearableActivity()");
        new StartWearableActivityTask().execute();
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node) {
        Log.d(TAG, "sendStartActivityMessage(" + node + ")");
        Wearable.MessageApi.sendMessage(googleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "sendStartActivityMessage() result: " + sendMessageResult.getStatus().toString());
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "StartWearableActivityTask";
        @Override
        protected Void doInBackground(Void... args) {
            Log.d(TAG, "doInBackground()");
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    private void sendAuthRequiredMessage(String node) {
        Log.d(TAG, "sendAuthRequiredMessage(" + node + ")");
        Wearable.MessageApi.sendMessage(googleApiClient, node, AUTH_REQUIRED_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "sendAuthRequiredMessage() result: " + sendMessageResult.getStatus().toString());
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class SendAuthRequiredMessageTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "SendAuthRequiredMessageTask";
        @Override
        protected Void doInBackground(Void... args) {
            Log.d(TAG, "doInBackground()");
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendAuthRequiredMessage(node);
            }
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

    public static class WearableAccountListener implements AccountListener, GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {
        private static final String TAG = "WearableAccountListener";

        private static final String ACCOUNT_DATA_PATH = "/account";

        private GoogleApiClient googleApiClient;

        @Override
        public void update() {
            Log.d(TAG, "update()");
            googleApiClient = new GoogleApiClient.Builder(App.context)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.d(TAG, "failed to connect to googleApiClient: " + connectionResult.toString());
                return;
            }

            Cursor cursor = App.context.getContentResolver().query(ACCOUNTS_CONTENT_URI, ACCOUNT_ALL_FIELDS, null, new String[]{}, null);
            BankAccount bankAccount = BankAccount.cursorToBankAccount(cursor);
            cursor.close();

            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithoutExposeAnnotation();
            Gson gson = builder.create();
            String accountJson = gson.toJson(bankAccount);

            PutDataRequest putDataRequest = PutDataRequest.create(ACCOUNT_DATA_PATH);
            putDataRequest.setData(accountJson.getBytes(StandardCharsets.UTF_8));
            PendingResult<DataApi.DataItemResult> dataItemResultPendingResult = Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);

            dataItemResultPendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.d(TAG, "DataApi.DataItemResult." + dataItemResult.toString());
                    if (! dataItemResult.getStatus().isSuccess()) {
                        Log.d(TAG, "putDataItem() failed:" + dataItemResult.getStatus().getStatusMessage());
                    }
                }
            });
        }

        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "onConnected()");
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended(" + cause + ")");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "onConnectionFailed(" + connectionResult + ")");

        }
    }
}
