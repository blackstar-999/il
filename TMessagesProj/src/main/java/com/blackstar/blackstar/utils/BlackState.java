/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.utils;

import android.util.LongSparseArray;
import com.blackstar.blackstar.BlackConfig;

import java.util.ArrayList;

public class BlackState {
    private static final BlackStateVariable allowReadPacket = new BlackStateVariable();
    private static final BlackStateVariable automaticallyScheduled = new BlackStateVariable();
    private static final BlackStateVariable hideSelection = new BlackStateVariable();
    private static final LongSparseArray<ArrayList<Integer>> deletePermitted = new LongSparseArray<>();

    public static void setAllowReadPacket(boolean val, int resetAfter) {
        allowReadPacket.val = val;
        allowReadPacket.resetAfter = resetAfter;
    }

    public static boolean getAllowReadPacket() {
        return BlackConfig.sendReadPackets || allowReadPacket.process();
    }

    public static void setAutomaticallyScheduled(boolean val, int resetAfter) {
        automaticallyScheduled.val = val;
        automaticallyScheduled.resetAfter = resetAfter;
    }

    public static boolean getAutomaticallyScheduled() {
        return automaticallyScheduled.process();
    }

    public static void setHideSelection(boolean val, int resetAfter) {
        hideSelection.val = val;
        hideSelection.resetAfter = resetAfter;
    }

    public static boolean getHideSelection() {
        return hideSelection.process();
    }

    public static void permitDeleteMessage(long dialogId, int messageId) {
        var list = deletePermitted.get(dialogId);
        if (list == null) {
            list = new ArrayList<>();
            deletePermitted.put(dialogId, list);
        }

        list.add(messageId);
    }

    public static boolean isDeletePermitted(long dialogId, int messageId) {
        var list = deletePermitted.get(dialogId);
        if (list == null) {
            return false;
        }

        return list.contains(messageId);
    }

    public static void messageDeleted(long dialogId, int messageId) {
        var list = deletePermitted.get(dialogId);
        if (list == null) {
            return;
        }

        list.remove((Object) messageId);
    }
}
