/*
 * This is the source code of Black StaR for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.blackstar.blackstar.sync;

import com.blackstar.blackstar.BlackUtils;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class BlackInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        var req = chain.request()
                .newBuilder()
                .addHeader("X-APP-PACKAGE", BlackUtils.getPackageName())
                .addHeader("Authorization", BlackSyncConfig.getToken())
                .build();

        return chain.proceed(req);
    }
}
