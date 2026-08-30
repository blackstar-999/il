/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync;

public class BlackSyncControllerEmpty extends BlackSyncController {
    @Override
    public void connect() {
        // nah
    }

    @Override
    public void forceSync() {
        // nah
    }

    @Override
    public void syncRead(int accountId, long dialogId, int untilId) {
        // nah
    }
}
