/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.delayedconfirmation;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class MainActivity extends Activity implements
        DelayedConfirmationView.DelayedConfirmationListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DelayedConfirmation";
    private static final int NUM_SECONDS = 5;

    private static final String TIMER_SELECTED_PATH = "/timer_selected";
    private static final String TIMER_FINISHED_PATH = "/timer_finished";

    private DelayedConfirmationView delayedConfirmationView;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main_activity);
        delayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.delayed_confirmation);
        delayedConfirmationView.setTotalTimeMs(NUM_SECONDS * 1000);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    /**
     * Starts the DelayedConfirmationView when user presses "Start Timer" button.
     */
    public void onStartTimer(View view) {
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_timer_selected))
                .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, notification);
        sendMessageToCompanion(TIMER_SELECTED_PATH);
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        finish();
    }

    @Override
    public void onTimerFinished(View v) {
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_timer_finished))
                .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, notification);
        sendMessageToCompanion(TIMER_FINISHED_PATH);
        finish();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google Api Client");
    }

    private void sendMessageToCompanion(final String path) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        for (final Node node : getConnectedNodesResult.getNodes()) {
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path,
                                    new byte[0]).setResultCallback(getSendMessageResultCallback());
                        }
                    }
                }
        );

    }

    private ResultCallback<MessageApi.SendMessageResult> getSendMessageResultCallback() {
        return new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Failed to connect to Google Api Client with status "
                            + sendMessageResult.getStatus());
                }
            }
        };
    }
}
