package com.kii.thing.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by German Viscuso <germanviscuso@gmail.com> on 12/6/14.
 */
public class GCMPreference {
    private static final String PREFERENCE_NAME = "KiiPush";
    private static final String PROPERTY_REG_ID = "GCMregId";

    public static String getRegistrationId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        return registrationId;
    }

    public static void setRegistrationId(Context context, String regId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.commit();
    }
}
