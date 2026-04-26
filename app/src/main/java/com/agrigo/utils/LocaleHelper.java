package com.agrigo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    private static final String PREFS_NAME = "AgriGo_Prefs";

    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, "en"); // Default to English if not set
        return setLocale(context, lang);
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedData(context, defaultLanguage);
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        return getPersistedData(context, Locale.getDefault().getLanguage());
    }

    public static Context setLocale(Context context, String language) {
        persist(context, language);
        android.util.Log.d("LocaleHelper", "Setting locale via AppCompatDelegate to: " + language);

        // Modern Android way (AppCompat 1.6+)
        androidx.core.os.LocaleListCompat appLocales = androidx.core.os.LocaleListCompat.forLanguageTags(language);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocales);

        // Fallback for immediate resource access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        }
        return updateResourcesLegacy(context, language);
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lang = preferences.getString(SELECTED_LANGUAGE, defaultLanguage);
        android.util.Log.d("LocaleHelper", "Persisted language: " + lang + " (default: " + defaultLanguage + ")");
        return lang;
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_LANGUAGE, language);
        editor.commit(); // Synchronous to ensure it's saved before restart
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 
                Locale.forLanguageTag(language) : new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            context = context.createConfigurationContext(config);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        
        // Also update application context resources for consistency
        Context appCtx = context.getApplicationContext();
        if (appCtx != null) {
            Resources appRes = appCtx.getResources();
            Configuration appConfig = new Configuration(appRes.getConfiguration());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appConfig.setLocale(locale);
            } else {
                appConfig.locale = locale;
            }
            appRes.updateConfiguration(appConfig, appRes.getDisplayMetrics());
        }

        return context;
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 
                Locale.forLanguageTag(language) : new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }
}
