/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.utils;

import org.telegram.tgnet.TLRPC;

public class BlackFileLocation extends TLRPC.FileLocation {
    public String path;

    public BlackFileLocation(String path) {
        this.path = path;
    }
}
