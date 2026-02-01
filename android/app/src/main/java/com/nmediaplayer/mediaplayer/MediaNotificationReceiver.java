package com.nmediaplayer.mediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.getcapacitor.Bridge;
import com.nmediaplayer.mediaplayer.plugins.MediaPlugin;

public class MediaNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if (action == null) return;

        Intent broadcastIntent = new Intent("com.nmediaplayer.MEDIA_ACTION");
        broadcastIntent.putExtra("action", action);
        context.sendBroadcast(broadcastIntent);
    }
}