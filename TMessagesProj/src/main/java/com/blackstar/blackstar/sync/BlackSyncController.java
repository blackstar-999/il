/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync;

import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.blackstar.blackstar.BlackConfig;
import com.blackstar.blackstar.BlackUtils;
import com.blackstar.blackstar.sync.models.*;
import com.blackstar.blackstar.utils.BlackGhostUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;

import java.io.IOException;
import java.util.HashMap;

public class BlackSyncController {

    private static final DispatchQueue queue = new DispatchQueue("BlackSyncController");
    private static BlackSyncController instance;
    private final HashMap<Long, Integer> accounts;
    private final OkHttpClient client;

    public BlackSyncController() {
        accounts = new HashMap<>();

        var clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new BlackInterceptor());
        client = clientBuilder.build();
    }

    public static void create() {
        if (instance != null && !(instance instanceof BlackSyncControllerEmpty)) {
            return;
        }

        if (!BlackConfig.syncEnabled) {
            BlackSyncWebSocketClient.nullifyInstance();
            instance = new BlackSyncControllerEmpty();
            return;
        }

        instance = new BlackSyncController();

        queue.postRunnable(() -> {
            instance.loadAccounts();
            instance.connect();
        });
    }

    public static BlackSyncController getInstance() {
        if (instance == null) {
            create();
        }

        return instance;
    }

    public static void nullifyInstance() {
        if (instance == null || instance instanceof BlackSyncControllerEmpty) {
            return;
        }

        BlackSyncWebSocketClient.nullifyInstance();

        BlackSyncState.setConnectionState(BlackSyncConnectionState.NotRegistered);

        instance = new BlackSyncControllerEmpty();
    }

    private static void enqueueRetry() {
        queue.postRunnable(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            create();
        });
    }

    private void loadAccounts() {
        accounts.clear();

        for (int id = 0; id < UserConfig.MAX_ACCOUNT_COUNT; id++) {
            var instance = UserConfig.getInstance(id);
            if (instance.isClientActivated()) {
                accounts.put(instance.getClientUserId(), id);
            }
        }
    }

    public void connect() {
        if (TextUtils.isEmpty(BlackSyncConfig.getToken())) {
            nullifyInstance();
            BlackSyncState.setConnectionState(BlackSyncConnectionState.NoToken);
            // don't enqueue retry
            // there will be an attempt when token changed
            return;
        }

        var self = getSelfForConnect();

        if (self == null) {
            nullifyInstance();
            BlackSyncState.setConnectionState(BlackSyncConnectionState.InvalidToken);
            // retry already enqueued if needed
            return;
        }

        // note for the code explorers:
        // yes, you can nullify this code in smali, but we have server side check,
        // so you can't sync without Black StaR MVP.
        if (!self.isMVP) {
            nullifyInstance();
            BlackSyncState.setConnectionState(BlackSyncConnectionState.NoMVP);
            enqueueRetry();
            return;
        }

        var deviceName = BlackUtils.getDeviceName();
        var deviceIdentifier = BlackUtils.getDeviceIdentifier();

        var url = BlackSyncConfig.getRegisterDeviceURL();

        var obj = new JsonObject();
        obj.addProperty("name", deviceName);
        obj.addProperty("identifier", deviceIdentifier);

        var body = RequestBody.create(obj.toString(), MediaType.get("application/json; charset=utf-8"));

        var request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        boolean success;
        try (var response = client.newCall(request).execute()) {
            success = response.isSuccessful();
            Log.d("BlackSync", "Register device status: " + response.code());
            BlackSyncState.setRegisterStatusCode(response.code());
        } catch (IOException e) {
            success = false;
            Log.d("BlackSync", "Failed to register device: " + e.getMessage());
        }

        if (success) {
            BlackSyncState.setConnectionState(BlackSyncConnectionState.Disconnected);
        }

        if (!success) {
            nullifyInstance();
            BlackSyncState.setConnectionState(BlackSyncConnectionState.NotRegistered);
            enqueueRetry();
            return;
        }

        var res = BlackSyncWebSocketClient.create();
        Log.d("BlackSync", "WebSocket client created: " + res);
    }

    private BlackUser getSelfForConnect() {
        var url = BlackSyncConfig.getUserDataURL();

        var request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try {
            var response = client.newCall(request).execute();

            Log.d("BlackSync", "Get self status: " + response.code());

            return new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ssz")
                    .create()
                    .fromJson(response.body().string(), BlackUser.class);
        } catch (Exception e) {
            Log.d("BlackSync", "Failed to get self: " + e.getMessage());
            enqueueRetry();
        }

        return null;
    }

    public void forceSync() {
        queue.postRunnable(this::forceSyncInner);
    }

    private void forceSyncInner() {
        var userId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();

        var url = BlackSyncConfig.getForceSyncURL();

        var obj = new JsonObject();
        obj.addProperty("userId", userId);
        obj.addProperty("fromDate", 0);

        var body = RequestBody.create(obj.toString(), MediaType.get("application/json; charset=utf-8"));

        var request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        boolean success;
        try {
            var response = client.newCall(request).execute();
            success = response.isSuccessful();
            Log.d("BlackSync", "Force sync status: " + response.code());
        } catch (IOException e) {
            success = false;
            Log.d("BlackSync", "Failed to force sync: " + e.getMessage());
        }
    }

    public void onForceSync(SyncForce req) {
        var accountId = accounts.get(req.userId);

        var controller = MessagesController.getInstance(accountId);

        // ~ sync dialog states
        var dialogs = controller.getAllDialogs();
        var readsBatchEvent = new SyncBatch();
        readsBatchEvent.userId = req.userId;

        readsBatchEvent.args = new SyncBatch.SyncBatchArgs();

        for (var dialog : dialogs) {
            var dialogId = dialog.id;
            var dialogReadMaxId = dialog.read_inbox_max_id;

            var unread = controller.getDialogUnreadCount(dialog);

            var readEv = new SyncRead();
            readEv.userId = req.userId;

            readEv.args = new SyncRead.SyncReadArgs();
            readEv.args.dialogId = dialogId;
            readEv.args.untilId = dialogReadMaxId;
            readEv.args.unread = unread;

            readsBatchEvent.args.events.add(readEv);
        }

        var readEventsMessage = new Gson().toJson(readsBatchEvent);
        BlackSyncWebSocketClient.getInstance().send(readEventsMessage);

        // ~ sync edits history
//        var editedMessageDao = BlackData.getEditedMessageDao();
//        var editedMessagesCount = editedMessageDao.getSyncCount(req.userId, req.args.fromDate);
//
//        var pages = Math.ceil(editedMessagesCount / 50.0);
//        for (var page = 0; page < pages; ++page) {
//            var edits = editedMessageDao.getForSync(req.userId, req.args.fromDate, page * 50);
//
//            var editsMessage = new Gson().toJson(edits);
//            BlackSyncWebSocketClient.getInstance().send(editsMessage);
//        }

        var finishEvent = new Gson().toJson(new SyncForceFinish());
        BlackSyncWebSocketClient.getInstance().send(finishEvent);
    }

    public void onBatchSync(JsonObject req) {
        for (var event : req.getAsJsonObject("args").get("events").getAsJsonArray()) {
            invokeHandler(event.getAsJsonObject());
        }
    }

    public void syncRead(int accountId, long dialogId, int untilId) {
        var controller = MessagesController.getInstance(accountId);
        var dialog = controller.getDialog(dialogId);
        var unread = controller.getDialogUnreadCount(dialog);

        var req = new SyncRead();
        req.userId = UserConfig.getInstance(accountId).getClientUserId();

        req.args = new SyncRead.SyncReadArgs();
        req.args.dialogId = dialogId;
        req.args.untilId = untilId;
        req.args.unread = unread;

        Log.d("BlackSync", "count: " + unread);

        var message = new Gson().toJson(req);

        Log.d("BlackSync", "Sending sync_read: " + message);
        BlackSyncWebSocketClient.getInstance().send(message);
    }

    public void onSyncRead(SyncRead req) {
        var accountId = accounts.get(req.userId);

        var controller = MessagesController.getInstance(accountId);

        var dialog = controller.getDialog(req.args.dialogId);
        if (dialog.unread_count <= req.args.unread) {
            return;
        }

        BlackGhostUtils.markReadLocally(accountId, req.args.dialogId, req.args.untilId, req.args.unread);
    }

    public void invokeHandler(JsonObject req) {
        Log.d("BlackSync", req.toString());

        var userId = req.get("userId").getAsLong();
        var type = req.get("type").getAsString();

        Log.d("BlackSync", "userId: " + userId + ", type: " + type);

        if (!accountExists(userId)) {
            Log.w("BlackSync", "Sync for unknown account: " + userId);
            return;
        }

        switch (type) {
            case "sync_force":
                onForceSync(new Gson().fromJson(req, SyncForce.class));
                break;
            case "sync_batch":
                onBatchSync(req);
                break;
            case "sync_read":
                onSyncRead(new Gson().fromJson(req, SyncRead.class));
                break;
            default:
                Log.d("BlackSync", "Unknown sync type: " + type);
                break;
        }
    }

    public boolean accountExists(long userId) {
        return userId == 0 || accounts.containsKey(userId);
    }
}
