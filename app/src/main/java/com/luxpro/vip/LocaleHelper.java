package com.luxpro.vip;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {

    public static final String LANG_AR = "ar";
    public static final String LANG_EN = "en";
    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    public static String toggleLanguage(Context context) {
        String currentLang = getLanguage(context);
        String newLang = currentLang.equals(LANG_AR) ? LANG_EN : LANG_AR;
        setLocale(context, newLang);
        return newLang;
    }

    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, "ar"); // Default is Arabic (ar)
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String language) {
        persist(context, language);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        }
        return updateResourcesLegacy(context, language);
    }

    public static String getLanguage(Context context) {
        return getPersistedData(context, "ar");
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = context.getSharedPreferences("LUX_PRO_PREFS", Context.MODE_PRIVATE);
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage);
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences("LUX_PRO_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_LANGUAGE, language);
        editor.apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);

        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);

        // Force layout direction dynamically based on locale (RTL/LTR)
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        configuration.setLayoutDirection(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }
}
