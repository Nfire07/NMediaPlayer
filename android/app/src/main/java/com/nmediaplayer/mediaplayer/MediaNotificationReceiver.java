package com.nmediaplayer.mediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MediaNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaNotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Intent broadcastIntent = new Intent("com.nmediaplayer.MEDIA_ACTION");
        broadcastIntent.putExtra("action", action);
        context.sendBroadcast(broadcastIntent);
    }
}