/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync;

import android.util.Log;
import com.blackstar.blackstar.BlackConstants;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;

public class BlackSyncState {
    private static int lastSent;
    private static int lastReceived;
    private static BlackSyncConnectionState connectionState = BlackSyncConnectionState.NotRegistered;
    private static int registerStatusCode;

    public static int getLastSent() {
        return lastSent;
    }

    public static void setLastSent(int lastSent) {
        BlackSyncState.lastSent = lastSent;

        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(BlackConstants.AYUSYNC_LAST_SENT_CHANGED));
    }

    public static int getLastReceived() {
        return lastReceived;
    }

    public static void setLastReceived(int lastReceived) {
        BlackSyncState.lastReceived = lastReceived;

        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(BlackConstants.AYUSYNC_LAST_RECEIVED_CHANGED));
    }

    public static BlackSyncConnectionState getConnectionState() {
        return connectionState;
    }

    public static void setConnectionState(BlackSyncConnectionState connectionState) {
        Log.d("BlackSync", "setConnectionState: " + connectionState);
        BlackSyncState.connectionState = connectionState;

        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(BlackConstants.AYUSYNC_STATE_CHANGED));
    }

    public static String getConnectionStateString() {
        String status;
        switch (getConnectionState()) {
            case Connected:
                status = LocaleController.getString(R.string.BlackSyncStatusOk);
                break;
            case Disconnected:
                status = LocaleController.getString(R.string.BlackSyncStatusErrorDisconnected);
                break;
            case NotRegistered:
                status = LocaleController.getString(R.string.BlackSyncStatusErrorNotRegistered);
                break;
            case NoToken:
                status = LocaleController.getString(R.string.BlackSyncStatusErrorNoToken);
                break;
            case InvalidToken:
                status = LocaleController.getString(R.string.BlackSyncStatusErrorInvalidToken);
                break;
            case NoMVP:
                status = LocaleController.getString(R.string.BlackSyncStatusErrorNoMVP);
                break;
            default:
                status = "unknown";
                break;
        }

        return status;
    }

    public static int getRegisterStatusCode() {
        return registerStatusCode;
    }

    public static void setRegisterStatusCode(int registerStatusCode) {
        BlackSyncState.registerStatusCode = registerStatusCode;

        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(BlackConstants.AYUSYNC_REGISTER_STATUS_CODE_CHANGED));
    }
}
