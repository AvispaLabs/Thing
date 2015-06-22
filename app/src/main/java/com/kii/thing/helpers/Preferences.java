package com.kii.thing.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * This class is a helper class for SharedPreferences operation
 */
public class Preferences {
    /**
     * Name of SharedPreferences
     */
    private static final String PREF_NAME = "settings";

    /**
     * Keys of SharedPreferences entry
     */
    interface Key {
        static final String APP_ID = "appId";
        static final String APP_KEY = "appKey";
        static final String SITE = "site";
        static final String STORED_ACCESS_TOKEN = "token";
    }

    /**
     * Save access token
     * @param context
     * @param token
     */
    public static void setStoredAccessToken(Context context, String token) {
        SharedPreferences pref = getSharedPreferences(context);
        Editor edit = pref.edit();
        edit.putString(Key.STORED_ACCESS_TOKEN, token);
        edit.commit();
    }

    /**
     * Get access token
     * @param context
     * @return null if token is not stored in SharedPreferences
     */
    public static String getStoredAccessToken(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        return pref.getString(Key.STORED_ACCESS_TOKEN, null);
    }

    /**
     * Clear access token
     * @param context
     */
    public static void clearStoredAccessToken(Context context) {
        SharedPreferences pref = getSharedPreferences(context);
        Editor edit = pref.edit();
        edit.remove(Key.STORED_ACCESS_TOKEN);
        edit.commit();
    }

    /**
     * @param context
     * @return instance of SharedPreferences
     */
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}