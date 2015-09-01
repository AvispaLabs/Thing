package com.kii.thing.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.kii.thing.MainActivity;
import com.kii.thing.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.kii.cloud.storage.*;
import com.kii.cloud.storage.callback.KiiObjectCallBack;

public class KiiPushBroadcastReceiver extends BroadcastReceiver {
    public final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            Bundle extras = intent.getExtras();
            ReceivedMessage message = PushMessageBundleHelper.parse(extras);
            KiiUser sender = message.getSender(); // null on direct push
            PushMessageBundleHelper.MessageType type = message.pushMessageType();
            Bundle bundle = message.getMessage();
            switch (type) {
                case PUSH_TO_APP:
                    PushToAppMessage appMsg = (PushToAppMessage) message;
                    Log.d(TAG, "Received PushToApp: " + appMsg.getMessage().toString());
                    //long when = appMsg.getMessage().getLong("when");
                    //String event_type = appMsg.getMessage().getString("type");
                    // Extract the target bucket and object.
                    Log.d(TAG, "Contains bucket: " + appMsg.containsKiiBucket());
                    Log.d(TAG, "Contains object: " + appMsg.containsKiiObject());
                    if (appMsg.containsKiiBucket()) {
                        Log.d(TAG, "PushToApp contains bucket");
                        KiiBucket bucket = appMsg.getKiiBucket();
                        if (appMsg.containsKiiObject()) {
                            Log.d(TAG, "PushToApp contains object");
                            KiiObject object = appMsg.getKiiObject();
                            // Refresh and fetch the latest key-value pairs.
                            object.refresh(new KiiObjectCallBack() {
                                @Override
                                public void onRefreshCompleted(int token, KiiObject object, Exception exception) {
                                    if (exception != null) {
                                        Log.e(TAG, "Could not refresh object from PushToApp: " + exception.toString());
                                        return;
                                    }
                                    String temperature = object.getString(Constants.THING_TEMP_FIELD);
                                    Log.d(TAG, "Received temperature: " + temperature);
                                    if (MainActivity.instance != null) {
                                        float temp = Float.valueOf(temperature);
                                        MainActivity.instance.addChartEntry(temp);
                                    }
                                }
                            });
                        }
                    }
                    break;
                case PUSH_TO_USER:
                    PushToUserMessage userMsg = (PushToUserMessage) message;
                    Log.d(TAG, "Received PushToUser: " + userMsg.getMessage().toString());
                    break;
                case DIRECT_PUSH:
                    DirectPushMessage directMsg = (DirectPushMessage) message;
                    Log.d(TAG, "Received DirectPush: " + directMsg.getMessage().toString());
                    for (String key : bundle.keySet()) {
                        Log.d(TAG, key + ":" + bundle.get(key).toString());
                    }
                    break;
            }
        }
    }
}
