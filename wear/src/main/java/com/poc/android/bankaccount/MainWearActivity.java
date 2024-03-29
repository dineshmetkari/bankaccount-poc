package com.poc.android.bankaccount;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static com.poc.android.bankaccount.service.DataLayerListenerService.ACCOUNT_BALANCE_ACTION;
import static com.poc.android.bankaccount.service.DataLayerListenerService.ACCOUNT_BALANCE_EXTRA;
import static com.poc.android.bankaccount.service.DataLayerListenerService.ACCOUNT_NAME_EXTRA;
import static com.poc.android.bankaccount.service.DataLayerListenerService.AUTH_REQUIRED_ACTION;

public class MainWearActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String TAG = "MainWearActivity";
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final String GET_BALANCE_PATH = "/get-account-balance";

    private boolean hasSpeechRecognizerRun = false;
    private SpeechRecognizer speechRecognizer;
    private GoogleApiClient googleApiClient;
    private ViewGroup accountBalanceContainer;
    private TextView accountNameTextView;
    private TextView accountBalanceTextView;


    private BroadcastReceiver accountBalanceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountNamePrefix = getString(R.string.account_name_prefix);
            String accountBalancePrefix = getString(R.string.account_balance_prefix);
            String accountName = intent.getStringExtra(ACCOUNT_NAME_EXTRA);
            String accountBalance = intent.getStringExtra(ACCOUNT_BALANCE_EXTRA);

            accountNameTextView.setText(accountNamePrefix + accountName);
            accountBalanceTextView.setText(accountBalancePrefix + accountBalance);

            accountBalanceContainer.setVisibility(View.VISIBLE);
        }
    };

    private BroadcastReceiver authRequiredBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            accountNameTextView.setText("");
            accountBalanceTextView.setText("");

            accountBalanceContainer.setVisibility(View.INVISIBLE);

            String message = getString(R.string.auth_required_title) + ": " + getString(R.string.auth_required_text);

            Toast.makeText(MainWearActivity.this, message, Toast.LENGTH_LONG).show();
        }
    };

    public static String getTopActivity(Context context) {
        List tasks = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1);

        if (tasks.isEmpty()) {
            return null;
        } else {
            ActivityManager.RunningTaskInfo taskInfo = (ActivityManager.RunningTaskInfo) tasks.get(0);
            return taskInfo.topActivity.getPackageName();
        }
    }

    public static boolean isMainActivityRunning(Context context) {
        boolean isRunning = false;

        List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo info : tasks) {
            if (info.topActivity.getPackageName().equalsIgnoreCase(context.getPackageName())) {
                isRunning = true;
                break;
            }
        }

        return isRunning;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                accountBalanceContainer = (ViewGroup) stub.findViewById(R.id.balance_container);
                accountNameTextView = (TextView) stub.findViewById(R.id.accountTextView);
                accountBalanceTextView = (TextView) stub.findViewById(R.id.balanceTextView);
            }
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

        registerReceiver(accountBalanceBroadcastReceiver, new IntentFilter(ACCOUNT_BALANCE_ACTION));
        registerReceiver(authRequiredBroadcastReceiver, new IntentFilter(AUTH_REQUIRED_ACTION));
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(accountBalanceBroadcastReceiver);
        unregisterReceiver(authRequiredBroadcastReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent(" + intent + ")");
        super.onNewIntent(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged(" + hasFocus + ")");
        if (hasFocus && ! hasSpeechRecognizerRun) {
            displaySpeechRecognizer(null);
        }
        super.onWindowFocusChanged(hasFocus);
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(" + bundle + ")");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(" + i + ")");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(" + connectionResult + ")");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(" + dataEvents + ")");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived(" + messageEvent + ")");
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "onPeerConnected(" + node + ")");
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "onPeerDisconnected(" + node + ")");
    }

    // Create an intent that can start the Speech Recognizer activity
    public void displaySpeechRecognizer(View view) {
        Log.d(TAG, "displaying speech recognizer...");
        hasSpeechRecognizerRun = true;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Banking Command or Help for list of Commands");
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
        Toast.makeText(MainWearActivity.this, "Speak Banking Voice Command", Toast.LENGTH_LONG).show();
        Toast.makeText(MainWearActivity.this, "Example: \"Get Balance\"", Toast.LENGTH_LONG).show();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void displaySpeechRecognizer2(View view) {
        Log.d(TAG, "displaying speech recognizer...");
        hasSpeechRecognizerRun = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Banking Command or Help for list of Commands");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        speechRecognizer.startListening(intent);
    }

    public void getBalance(View view) {
        Log.d(TAG, "sending balance request");
        new SendGetBalanceTask().execute();
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + ", " + requestCode + ", " + data + ")");
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            if (spokenText.toUpperCase().contains("BALANCE")) {
                Log.d(TAG, "sending balance request");
                new SendGetBalanceTask().execute();
            } else {
                Toast.makeText(MainWearActivity.this, "Unknown voice command: "  + spokenText, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class SendGetBalanceTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "SendGetBalanceTask";
        @Override
        protected Void doInBackground(Void... args) {
            Log.d(TAG, "doInBackground()");
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendGetBalanceRequest(node);
            }
            return null;
        }
    }

    private void sendGetBalanceRequest(String node) {
        Log.d(TAG, "sendGetBalanceRequest(" + node + ")");
        Wearable.MessageApi.sendMessage(googleApiClient, node, GET_BALANCE_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "sendGetBalanceRequest() result: " + sendMessageResult.getStatus().toString());
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }
    class SpeechRecognitionListener implements RecognitionListener {
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error) {
            Log.d(TAG,  "error " +  error);
        }
        public void onResults(Bundle results) {
            @SuppressWarnings("UnusedDeclaration") String str = "";
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (Object aData : data) {
                Log.d(TAG, "result " + aData);
                str += aData;
            }
        }
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }
    }
}
