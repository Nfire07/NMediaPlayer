package com.nmediaplayer.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.core.app.NotificationCompat;

public class MusicPlayerService extends Service {
    private static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Actions
    public static final String ACTION_PLAY = "com.nmediaplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.nmediaplayer.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.nmediaplayer.ACTION_NEXT";
    public static final String ACTION_PREV = "com.nmediaplayer.ACTION_PREV";
    public static final String ACTION_STOP = "com.nmediaplayer.ACTION_STOP";
    
    public static final String ACTION_START_FOREGROUND = "com.nmediaplayer.START_FOREGROUND";
    public static final String ACTION_UPDATE_NOTIFICATION = "com.nmediaplayer.UPDATE_NOTIFICATION";
    public static final String ACTION_STOP_FOREGROUND = "com.nmediaplayer.STOP_FOREGROUND";
    
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_IS_PLAYING = "isPlaying";

    private String currentTitle = "No song playing";
    private String currentArtist = "Unknown Artist";
    private boolean isPlaying = false;
    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            if (ACTION_START_FOREGROUND.equals(action)) {
                currentTitle = intent.getStringExtra(EXTRA_TITLE);
                currentArtist = intent.getStringExtra(EXTRA_ARTIST);
                isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true);
                startForeground(NOTIFICATION_ID, createNotification());
            } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
                currentTitle = intent.getStringExtra(EXTRA_TITLE);
                currentArtist = intent.getStringExtra(EXTRA_ARTIST);
                isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying);
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, createNotification());
                }
            } else if (ACTION_STOP_FOREGROUND.equals(action)) {
                stopForeground(true);
                stopSelf();
            } else {
                handleMediaAction(action);
            }
        }
        return START_NOT_STICKY;
    }

    private void handleMediaAction(String action) {
        Intent broadcastIntent = new Intent("com.nmediaplayer.MEDIA_ACTION");
        broadcastIntent.putExtra("action", action);
        sendBroadcast(broadcastIntent);
    }

    private Notification createNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class); 
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true);

        // Action Previous (0)
        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREV));

        // Action Play/Pause (1)
        if (isPlaying) {
            builder.addAction(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else {
            builder.addAction(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
        }

        // Action Next (2)
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        
        // Action Stop (3)
        builder.addAction(generateAction(android.R.drawable.ic_delete, "Stop", ACTION_STOP));

        // Stile Media
        androidx.media.app.NotificationCompat.MediaStyle mediaStyle = 
            new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2);
        
        builder.setStyle(mediaStyle);

        return builder.build();
    }

    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(this, MusicPlayerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(this, intentAction.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action(icon, title, pendingIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controlli riproduzione musicale");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
}