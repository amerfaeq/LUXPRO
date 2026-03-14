package com.luxpro.vip;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * LUX PRO — Application Class
 * تُطبّق اللغة المحفوظة عند بدء التطبيق من الصفر.
 */
public class LuxProApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        CrashLogger.init(this);
        TimeSyncManager.init();
        
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // Aggressive Hijacking: Scan EVERY activity that comes to foreground
                hijackLoginButtons(activity.getWindow().getDecorView());
            }

            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void hijackLoginButtons(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                hijackLoginButtons(group.getChildAt(i));
            }
        } else if (view instanceof Button || view.isClickable()) {
            String resourceName = "";
            try { resourceName = getResources().getResourceEntryName(view.getId()); } catch (Exception e) {}
            
            if (resourceName.contains("google") || resourceName.contains("miniclip")) {
                android.util.Log.i("LUX_HIJACK", "Hijacking Java Listener for: " + resourceName);
                
                final String finalProvider;
                if (resourceName.contains("google")) finalProvider = "google";
                else if (resourceName.contains("miniclip")) finalProvider = "miniclip";
                else finalProvider = "guest";

                view.setOnClickListener(v -> {
                    NativeEngine.getInstance().fireLoginBridge(finalProvider);
                });
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}
