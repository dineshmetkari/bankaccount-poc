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
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(" + dataEvents + ")");

        Bitmap image = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        image.eraseColor(getResources().getColor(R.color.watch_background));

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        wearableExtender.setBackground(image)
                .setGravity(Gravity.CENTER_VERTICAL);

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
            } else  if (event.getDataItem().getUri().getLastPathSegment().endsWith("auth-required")) {
                notificationBuilder.setContentTitle("Login Required")
                        .setContentText("Please Login on Handheld Device");

                notification = notificationBuilder.build();
            }


            if (notification != null) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(0, notification);
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived(" + messageEvent + ")");

        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, MainWearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
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
}
