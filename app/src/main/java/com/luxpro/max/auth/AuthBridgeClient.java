package com.luxpro.max.auth;

import android.content.Context;
import com.chuckerteam.chucker.api.ChuckerCollector;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.chuckerteam.chucker.api.RetentionManager;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class AuthBridgeClient {
    private static OkHttpClient client;

    public static synchronized OkHttpClient getInstance(Context context) {
        if (client == null) {
            // Configure Chucker Collector
            ChuckerCollector chuckerCollector = new ChuckerCollector(
                    context,
                    true, // Show notification
                    RetentionManager.Period.ONE_HOUR
            );

            // Create Chucker Interceptor
            ChuckerInterceptor chuckerInterceptor = new ChuckerInterceptor.Builder(context)
                    .collector(chuckerCollector)
                    .maxContentLength(250000L)
                    .redactHeaders("Authorization", "Cookie")
                    .alwaysReadResponseBody(true)
                    .build();

            // Build OkHttpClient
            client = new OkHttpClient.Builder()
                    .addInterceptor(chuckerInterceptor)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}
