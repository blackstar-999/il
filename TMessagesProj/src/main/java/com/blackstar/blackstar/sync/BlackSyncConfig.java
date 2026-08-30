/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync;

import com.blackstar.blackstar.BlackConfig;

public class BlackSyncConfig {
    private static String getWebSocketProtocol() {
        return BlackConfig.useSecureConnection ? "wss://" : "ws://";
    }

    private static String getHTTPProtocol() {
        return BlackConfig.useSecureConnection ? "https://" : "http://";
    }

    public static String getWebSocketURL() {
        return getWebSocketProtocol() + BlackConfig.getSyncServerURL() + "/sync/ws/v1";
    }

    public static String getUserDataURL() {
        return getHTTPProtocol() + BlackConfig.getSyncServerURL() + "/user/v1";
    }

    public static String getRegisterDeviceURL() {
        return getHTTPProtocol() + BlackConfig.getSyncServerURL() + "/sync/register/v1";
    }

    public static String getForceSyncURL() {
        return getHTTPProtocol() + BlackConfig.getSyncServerURL() + "/sync/force/v1";
    }

    public static String getToken() {
        return BlackConfig.getSyncServerToken();
    }

    public static String getProfileURL() {
        return getHTTPProtocol() + BlackConfig.getSyncServerURL() + "/ui/profile?token=" + BlackConfig.getSyncServerToken();
    }
}
