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
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;

public class MusicPlayerService extends Service {

    private static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1001;

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

        if (intent == null) return START_STICKY;

        String action = intent.getAction();

        if (ACTION_START_FOREGROUND.equals(action)) {

            String title = intent.getStringExtra(EXTRA_TITLE);
            String artist = intent.getStringExtra(EXTRA_ARTIST);

            if (title != null) currentTitle = title;
            if (artist != null) currentArtist = artist;

            isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true);

            startForeground(NOTIFICATION_ID, createNotification());

        } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {

            String title = intent.getStringExtra(EXTRA_TITLE);
            String artist = intent.getStringExtra(EXTRA_ARTIST);

            if (title != null) currentTitle = title;
            if (artist != null) currentArtist = artist;

            isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFICATION_ID, createNotification());

        } else if (ACTION_STOP_FOREGROUND.equals(action) || ACTION_STOP.equals(action)) {

            stopForeground(true);
            stopSelf();

        } else {
            handleMediaAction(action);
        }

        return START_STICKY;
    }

    private void handleMediaAction(String action) {
        Intent broadcastIntent = new Intent("com.nmediaplayer.MEDIA_ACTION");
        broadcastIntent.putExtra("action", action);
        sendBroadcast(broadcastIntent);
    }

    private Notification createNotification() {

        updatePlaybackState();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        Intent prevIntent = new Intent(this, MusicPlayerService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(
                this, 1, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent);

        Intent playPauseIntent = new Intent(this, MusicPlayerService.class);
        playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 2, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int playPauseIcon = isPlaying ?
                android.R.drawable.ic_media_pause :
                android.R.drawable.ic_media_play;

        builder.addAction(playPauseIcon,
                isPlaying ? "Pause" : "Play",
                playPausePendingIntent);

        Intent nextIntent = new Intent(this, MusicPlayerService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2);

        builder.setStyle(mediaStyle);

        return builder.build();
    }

    private void updatePlaybackState() {

        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                        isPlaying ?
                                PlaybackStateCompat.STATE_PLAYING :
                                PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f
                )
                .build();

        mediaSession.setPlaybackState(state);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Music playback controls");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
}