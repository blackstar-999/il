/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync;

import com.google.android.exoplayer2.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.blackstar.blackstar.BlackConfig;
import com.blackstar.blackstar.BlackUtils;
import dev.gustavoavila.websocketclient.WebSocketClient;
import dev.gustavoavila.websocketclient.exceptions.InvalidServerHandshakeException;

import java.net.URI;
import java.net.URISyntaxException;

public class BlackSyncWebSocketClient extends WebSocketClient {

    private static BlackSyncWebSocketClient instance;

    private BlackSyncWebSocketClient(URI uri) {
        super(uri);
    }

    public static boolean create() {
        if (instance != null) {
            return true;
        }

        URI url;
        try {
            url = new URI(BlackSyncConfig.getWebSocketURL());
        } catch (URISyntaxException e) {
            return false;
        }

        Log.d("BlackSync", "Creating new WebSocket client");

        instance = new BlackSyncWebSocketClient(url);

        instance.setConnectTimeout(5000);
        instance.setReadTimeout(60000);
        instance.addHeader("X-APP-PACKAGE", BlackUtils.getPackageName());
        instance.addHeader("X-DEVICE-IDENTIFIER", BlackUtils.getDeviceIdentifier());
        instance.addHeader("Authorization", BlackSyncConfig.getToken());
        instance.enableAutomaticReconnection(1500);
        instance.connect();

        return true;
    }

    public static BlackSyncWebSocketClient getInstance() {
        if (instance == null) {
            create();
        }

        return instance;
    }

    public static void nullifyInstance() {
        if (instance == null) {
            return;
        }

        BlackSyncState.setConnectionState(BlackSyncConnectionState.Disconnected);

        try {
            // crashed once with "java.lang.IllegalStateException: Timer already cancelled."
            instance.close(200, 0, "nullified");
        } catch (Exception e) {
            Log.e("BlackSync", "Error while closing WebSocket", e);
        }

        instance = null;
    }

    @Override
    public void send(String message) {
        try {
            super.send(message);

            BlackSyncState.setLastSent((int) (System.currentTimeMillis() / 1000));
        } catch (Exception e) {
            Log.e("BlackSync", "Error while sending message", e);
        }
    }

    @Override
    public void onOpen() {
        BlackSyncState.setConnectionState(BlackSyncConnectionState.Connected);

        Log.d("BlackSync", "Connected to the origin");
    }

    @Override
    public void onTextReceived(String message) {
        BlackSyncState.setLastReceived((int) (System.currentTimeMillis() / 1000));

        try {
            var response = new Gson().fromJson(message, JsonObject.class);
            BlackSyncController.getInstance().invokeHandler(response);
        } catch (Exception e) {
            Log.e("BlackSync", "Error while invoking handler", e);
        }
    }

    @Override
    public void onBinaryReceived(byte[] data) {
        Log.d("BlackSync", "binary received");
    }

    @Override
    public void onPingReceived(byte[] data) {
//        Log.d("BlackSync", "ping!");
    }

    @Override
    public void onPongReceived(byte[] data) {
//        Log.d("BlackSync", "pong!");
    }

    @Override
    public void onException(Exception e) {
        BlackSyncState.setConnectionState(BlackSyncConnectionState.Disconnected);

        Log.e("BlackSync", e.toString());

        if ((e instanceof InvalidServerHandshakeException) && BlackConfig.syncEnabled && instance == this) {
            // this fucking library doesn't support any other exception except `IOException`
            // so we have to reinitialize instance

            // using reflection call doesn't work

            BlackSyncController.nullifyInstance();

            try {
                Thread.sleep(1500);
            } catch (Exception e2) {
                Log.d("BlackSync", "jaBBa", e2);
            }

            BlackSyncController.create();
        }
    }

    @Override
    public void onCloseReceived(int reason, String description) {
        BlackSyncState.setConnectionState(BlackSyncConnectionState.Disconnected);

        Log.d("BlackSync", "Disconnected from the origin: " + description);
    }
}
