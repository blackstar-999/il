/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync.models;

public class SyncForceFinish implements SyncEvent {
    public String type = "sync_force_finish";
    public long userId;
    public SyncForceFinish.SyncForceFinishArgs args;

    public static class SyncForceFinishArgs {
    }
}
