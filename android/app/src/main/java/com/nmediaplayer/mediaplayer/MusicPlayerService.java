package com.nmediaplayer.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

public class MusicPlayerService extends Service {
    private static final String TAG = "MusicPlayerService";
    private static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_PLAY = "com.nmediaplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.nmediaplayer.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.nmediaplayer.ACTION_RESUME";
    public static final String ACTION_NEXT = "com.nmediaplayer.ACTION_NEXT";
    public static final String ACTION_PREV = "com.nmediaplayer.ACTION_PREV";
    public static final String ACTION_STOP = "com.nmediaplayer.ACTION_STOP";
    public static final String ACTION_SEEK = "com.nmediaplayer.ACTION_SEEK";
    
    public static final String ACTION_AUTO_NEXT_STARTED = "com.nmediaplayer.ACTION_AUTO_NEXT_STARTED";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_POSITION = "position";
    
    public static final String EXTRA_NEXT_PATH = "next_path";
    public static final String EXTRA_NEXT_TITLE = "next_title";
    public static final String EXTRA_NEXT_ARTIST = "next_artist";

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private String currentTitle = "No song";
    private String currentArtist = "";
    
    private String nextPath = null;
    private String nextTitle = null;
    private String nextArtist = null;

    private PowerManager.WakeLock wakeLock;
    private static MusicPlayerService instance;

    public static MusicPlayerService getInstance() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel(); // Ora questo metodo esiste (vedi sotto)
        initMediaSession();          // Ora questo metodo esiste (vedi sotto)
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicPlayer::WakeLock");
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction(), intent);
        }
        return START_NOT_STICKY;
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case ACTION_PLAY:
                if (intent != null) {
                    String path = intent.getStringExtra(EXTRA_PATH);
                    currentTitle = intent.getStringExtra(EXTRA_TITLE);
                    currentArtist = intent.getStringExtra(EXTRA_ARTIST);
                    
                    nextPath = intent.getStringExtra(EXTRA_NEXT_PATH);
                    nextTitle = intent.getStringExtra(EXTRA_NEXT_TITLE);
                    nextArtist = intent.getStringExtra(EXTRA_NEXT_ARTIST);
                    
                    playAudio(path);
                }
                break;
            case ACTION_RESUME:
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    acquireWakeLock();
                    mediaPlayer.start();
                    updateUI(true);
                }
                break;
            case ACTION_PAUSE:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    releaseWakeLock();
                    updateUI(false);
                }
                break;
            case ACTION_STOP:
                stopPlayer();
                break;
            case ACTION_SEEK:
                 if (mediaPlayer != null && intent != null) {
                    int posSec = intent.getIntExtra(EXTRA_POSITION, -1);
                    if (posSec >= 0) {
                        mediaPlayer.seekTo(posSec * 1000);
                        updatePlaybackState(mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
                    }
                }
                break;
            case ACTION_NEXT:
                notifyPlugin(ACTION_NEXT);
                break;
            case ACTION_PREV:
                notifyPlugin(ACTION_PREV);
                break;
        }
    }

    private void playAudio(String path) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(path);
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer Error: " + what);
                tryPlayNextAuto();
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Song finished.");
                if (!tryPlayNextAuto()) {
                    notifyPlugin(ACTION_NEXT);
                }
            });

            mediaPlayer.prepare(); 
            acquireWakeLock();
            mediaPlayer.start();
            
            startForeground(NOTIFICATION_ID, createNotification(true));
            updateUI(true);

        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
            tryPlayNextAuto();
        }
    }
    
    private boolean tryPlayNextAuto() {
        if (nextPath != null && !nextPath.isEmpty()) {
            Log.d(TAG, "Auto-playing next song: " + nextTitle);
            
            String path = nextPath;
            currentTitle = (nextTitle != null) ? nextTitle : "Unknown";
            currentArtist = (nextArtist != null) ? nextArtist : "Unknown";
            
            nextPath = null;
            nextTitle = null;
            nextArtist = null;
            
            playAudio(path);
            notifyPlugin(ACTION_AUTO_NEXT_STARTED);
            return true;
        }
        return false;
    }

    private void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        releaseWakeLock();
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        stopForeground(true);
        stopSelf();
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(); 
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    
    private void notifyPlugin(String action) {
        Intent intent = new Intent("com.nmediaplayer.MEDIA_ACTION");
        intent.putExtra("action", action);
        sendBroadcast(intent);
    }
    
    private void updateUI(boolean isPlaying) {
        updatePlaybackState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(isPlaying));
        }
        if (mediaPlayer != null) {
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration())
                    .build());
        }
    }

    private void updatePlaybackState(int state) {
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(state, mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0, 1.0f);
        mediaSession.setPlaybackState(builder.build());
    }
    
    // --- METODI AGGIUNTI QUI SOTTO ---

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Music Player Controls");
            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { handleAction(ACTION_RESUME, null); }
            @Override
            public void onPause() { handleAction(ACTION_PAUSE, null); }
            @Override
            public void onSkipToNext() { handleAction(ACTION_NEXT, null); }
            @Override
            public void onSkipToPrevious() { handleAction(ACTION_PREV, null); }
            @Override
            public void onStop() { handleAction(ACTION_STOP, null); }
            @Override
            public void onSeekTo(long pos) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int) pos);
                    updatePlaybackState(mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
                }
            }
        });
        mediaSession.setActive(true);
    }

    private Notification createNotification(boolean isPlaying) {
        Intent prevIntent = new Intent(this, MusicPlayerService.class); prevIntent.setAction(ACTION_PREV);
        PendingIntent pPrev = PendingIntent.getService(this, 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicPlayerService.class); nextIntent.setAction(ACTION_NEXT);
        PendingIntent pNext = PendingIntent.getService(this, 20, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MusicPlayerService.class); playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_RESUME);
        PendingIntent pPlayPause = PendingIntent.getService(this, 30, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MusicPlayerService.class); stopIntent.setAction(ACTION_STOP);
        PendingIntent pStop = PendingIntent.getService(this, 40, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2); 

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setSmallIcon(android.R.drawable.ic_media_play) 
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying) 
                .setStyle(mediaStyle)
                .addAction(android.R.drawable.ic_media_previous, "Prev", pPrev) 
                .addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, isPlaying ? "Pause" : "Play", pPlayPause)
                .addAction(android.R.drawable.ic_media_next, "Next", pNext)
                .addAction(android.R.drawable.ic_delete, "Stop", pStop)
                .build();
    }
    
    public int getCurrentPosition() {
        if (mediaPlayer != null) return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    
    @Override
    public android.os.IBinder onBind(Intent intent) { return null; }
    
    @Override
    public void onDestroy() {
        instance = null;
        releaseWakeLock();
        if (mediaPlayer != null) mediaPlayer.release();
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }
}