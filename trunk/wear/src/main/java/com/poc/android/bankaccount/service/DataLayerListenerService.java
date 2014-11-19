package com.poc.android.bankaccount.service;

import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Gravity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.poc.android.bankaccount.MainWearActivity;
import com.poc.android.bankaccount.R;
import com.poc.android.bankaccount.library.model.BankAccount;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

public class DataLayerListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DataLayerListenerService";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String AUTH_REQUIRED_PATH = "/auth-required";
    private static final int ACCOUNT_BALANCE_NOTIFICATION = 0;
    private static final int AUTH_REQUIRED_NOTIFICATION = 1;

    public static final String ACCOUNT_BALANCE_ACTION = "account_balance_action";
    public static final String ACCOUNT_NAME_EXTRA = "account_name_extra";
    public static final String ACCOUNT_BALANCE_EXTRA = "account_balance_extra";
    public static final String AUTH_REQUIRED_ACTION = "auth_required_action";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onCreate()");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand(" + intent + ", " + flags + ", " + startId + ")");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(" + dataEvents + ")");

        logActivityDetails();

        Bitmap image = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        image.eraseColor(getResources().getColor(R.color.watch_background));

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        wearableExtender.setBackground(image).setGravity(Gravity.CENTER_VERTICAL);

        Notification notification = null;

        for (DataEvent event : dataEvents) {
            Log.d(TAG, "data event uri:" + event.getDataItem().getUri());
            Log.d(TAG, "data event data: " + new String(event.getDataItem().getData(), StandardCharsets.UTF_8));

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setPriority(1)
                    .extend(wearableExtender);

            if (event.getDataItem().getUri().getLastPathSegment().endsWith("account")) {
                GsonBuilder builder = new GsonBuilder();
                builder.excludeFieldsWithoutExposeAnnotation();
                Gson gson = builder.create();

                BankAccount bankAccount = gson.fromJson(new String(event.getDataItem().getData(), StandardCharsets.UTF_8), BankAccount.class);
                Log.d(TAG, "BankAccount = " + bankAccount);

                NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

                double amount = bankAccount.getBalance();
                amount = amount / 100;

                notificationBuilder.setContentTitle("Account Balance")
                        .setContentText("Account " + bankAccount.getName() + " has a balance of " + numberFormat.format(amount));

                notification = notificationBuilder.build();

                //broadcast account info to activity
                Intent intent = new Intent(ACCOUNT_BALANCE_ACTION);
                intent.putExtra(ACCOUNT_NAME_EXTRA, bankAccount.getName());
                intent.putExtra(ACCOUNT_BALANCE_EXTRA, numberFormat.format(amount));
                sendBroadcast(intent);

            } else  if (event.getDataItem().getUri().getLastPathSegment().endsWith("auth-required")) {
                notificationBuilder.setContentTitle("Login Required")
                        .setContentText("Please Login on Handheld Device");

                notification = notificationBuilder.build();
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

            notificationManager.cancel(AUTH_REQUIRED_NOTIFICATION);

            if (notification != null) {
                notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(ACCOUNT_BALANCE_NOTIFICATION, notification);
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived(" + messageEvent + ")");

        logActivityDetails();

        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            Log.d(TAG, "start activity message received");
            Intent startIntent = new Intent(this, MainWearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(startIntent);
        } else if (messageEvent.getPath().equals(AUTH_REQUIRED_PATH)) {
            Log.d(TAG, "auth required message received");

            // post notification and then send a broadcast so the Activity can
            // display some "auth required" messaging
            Bitmap image = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
            image.eraseColor(getResources().getColor(R.color.watch_background));

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
            wearableExtender.setBackground(image).setGravity(Gravity.CENTER_VERTICAL);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setPriority(1)
                    .extend(wearableExtender);

            notificationBuilder.setContentTitle(getString(R.string.auth_required_title))
                    .setContentText(getString(R.string.auth_required_text));

            Notification notification = notificationBuilder.build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            notificationManager.notify(AUTH_REQUIRED_NOTIFICATION, notification);

            //broadcast auth required message
            Intent intent = new Intent(AUTH_REQUIRED_ACTION);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected(" + peer + ")");
        super.onPeerConnected(peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "onPeerDisconnected(" + peer + ")");
        super.onPeerDisconnected(peer);
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

    private void logActivityDetails() {
        Log.d(TAG, "top activity: " + MainWearActivity.getTopActivity(this));
        Log.d(TAG, "is " + getPackageName() + " running: " + MainWearActivity.isMainActivityRunning(this));
    }
}
