package com.luxpro.max.auth;

import android.content.Context;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

public class CustomTabHelper {

    /**
     * Opens a URL in a Chrome Custom Tab with customized styling.
     *
     * @param context The Context used to launch the intent.
     * @param url     The OAuth login URL.
     */
    public static void openLoginTab(Context context, String url) {
        // Define your custom color (e.g., Neon Blue)
        int neonBlue = 0xFF00FFFF;

        CustomTabColorSchemeParams colorSchemeParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(neonBlue)
                .setNavigationBarColor(0xFF000000) // Black nav bar
                .build();

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorSchemeParams)
                .setShowTitle(true) // Shows the page title in the toolbar
                .build();

        customTabsIntent.launchUrl(context, Uri.parse(url));
    }
}
