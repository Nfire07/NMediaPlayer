package com.nmusicplayer.nplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private static final String TAG = "MusicPlayerService";
    private static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_PLAY = "com.nmusicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.nmusicplayer.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.nmusicplayer.ACTION_RESUME";
    public static final String ACTION_NEXT = "com.nmusicplayer.ACTION_NEXT";
    public static final String ACTION_PREV = "com.nmusicplayer.ACTION_PREV";
    public static final String ACTION_STOP = "com.nmusicplayer.ACTION_STOP";
    public static final String ACTION_SEEK = "com.nmusicplayer.ACTION_SEEK";
    
    public static final String ACTION_PLAY_INDEX = "com.nmusicplayer.ACTION_PLAY_INDEX";
    public static final String ACTION_AUTO_NEXT_STARTED = "com.nmusicplayer.ACTION_AUTO_NEXT_STARTED";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_INDEX = "index";

    public static class SongItem {
        String path;
        String title;
        String artist;
        
        public SongItem(String path, String title, String artist) {
            this.path = path;
            this.title = title;
            this.artist = artist;
        }
    }

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private ArrayList<SongItem> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private String currentTitle = "";
    private String currentArtist = "";
    private PowerManager.WakeLock wakeLock;
    private static MusicPlayerService instance;
    
    private boolean isPrepared = false;
    private boolean isAutoNextPending = false;

    public static MusicPlayerService getInstance() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicPlayer::WakeLock");
        wakeLock.setReferenceCounted(false);

        initMediaSession();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction(), intent);
        }
        return START_NOT_STICKY;
    }

    public void setPlaylist(List<SongItem> newSongs) {
        this.playlist.clear();
        this.playlist.addAll(newSongs);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer Error: " + what + ", " + extra);
        isPrepared = false;
        mediaPlayer.reset();
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        acquireWakeLock();
        mp.start();
        
        startForeground(NOTIFICATION_ID, createNotification(true));
        updateUI(true);

        if (isAutoNextPending) {
            notifyPlugin(ACTION_AUTO_NEXT_STARTED);
            isAutoNextPending = false;
        } else {
            notifyPlugin(ACTION_PLAY);
        }
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case ACTION_PLAY:
                if (intent != null) {
                    String path = intent.getStringExtra(EXTRA_PATH);
                    currentTitle = intent.getStringExtra(EXTRA_TITLE);
                    currentArtist = intent.getStringExtra(EXTRA_ARTIST);
                    int sentIndex = intent.getIntExtra(EXTRA_INDEX, -1);
                    
                    if (sentIndex != -1) {
                        currentIndex = sentIndex;
                    } else {
                        syncIndexByPath(path);
                    }
                    playAudio(path, false);
                }
                break;
            case ACTION_RESUME:
                if (mediaPlayer != null && !mediaPlayer.isPlaying() && isPrepared) {
                    acquireWakeLock();
                    mediaPlayer.start();
                    updateUI(true);
                    notifyPlugin(ACTION_RESUME);
                }
                break;
            case ACTION_PAUSE:
                if (mediaPlayer != null && mediaPlayer.isPlaying() && isPrepared) {
                    mediaPlayer.pause();
                    releaseWakeLock();
                    updateUI(false);
                    notifyPlugin(ACTION_PAUSE);
                }
                break;
            case ACTION_STOP:
                stopPlayer();
                break;
            case ACTION_SEEK:
                if (mediaPlayer != null && isPrepared && intent != null) {
                    int posSec = intent.getIntExtra(EXTRA_POSITION, -1);
                    if (posSec >= 0) {
                        mediaPlayer.seekTo(posSec * 1000);
                    }
                }
                break;
            case ACTION_NEXT:
                playNext(false);
                break;
            case ACTION_PREV:
                playPrev();
                break;
        }
    }

    private void syncIndexByPath(String path) {
        if (path == null) return;
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).path.equals(path)) {
                currentIndex = i;
                break;
            }
        }
    }

    private void playNext(boolean isAuto) {
        if (playlist.isEmpty()) {
            notifyPlugin(ACTION_STOP);
            return;
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0; 
        }

        playSongAtIndex(nextIndex, isAuto); 
    }

    private void playPrev() {
        if (playlist.isEmpty()) return;
        if (mediaPlayer != null && isPrepared && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
            return;
        }
        int prevIndex = currentIndex - 1;
        if (prevIndex < 0) prevIndex = playlist.size() - 1;
        playSongAtIndex(prevIndex, false);
    }

    private void playSongAtIndex(int index, boolean isAuto) {
        if (playlist.isEmpty()) return;
        
        if (index < 0 || index >= playlist.size()) index = 0;
        
        currentIndex = index;
        SongItem song = playlist.get(currentIndex);
        
        currentTitle = song.title;
        currentArtist = song.artist;

        Intent intent = new Intent("com.nmusicplayer.TRACK_CHANGE");
        intent.putExtra("index", currentIndex);
        intent.putExtra("title", song.title);
        intent.putExtra("artist", song.artist);
        intent.putExtra("isPlaying", true);
        intent.setPackage(getPackageName());
        
        sendBroadcast(intent); 
        
        updateNotification(); 
        
        playAudio(song.path, isAuto);
    }

    private void updateNotification() {
        updateUI(true);
    }

    private void playAudio(String path, boolean isAuto) {
        isPrepared = false;
        isAutoNextPending = isAuto;

        try {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                } catch (Exception e) {}
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            }

            mediaPlayer.setDataSource(path);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync(); 

        } catch (IOException e) {
            Log.e(TAG, "Error playAudio (IO)", e);
        } catch (Exception e) {
            Log.e(TAG, "Error generic playAudio", e);
        }
    }

    private void stopPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.reset();
            } catch (Exception e) { e.printStackTrace(); }
        }
        isPrepared = false;
        releaseWakeLock();
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        stopForeground(true);
        stopSelf();
        notifyPlugin(ACTION_STOP);
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
        Intent intent = new Intent("com.nmusicplayer.MEDIA_ACTION");
        intent.putExtra("action", action);
        if (!playlist.isEmpty() && currentIndex >= 0 && currentIndex < playlist.size()) {
            SongItem current = playlist.get(currentIndex);
            intent.putExtra("currentIndex", currentIndex);
            intent.putExtra("currentPath", current.path);
            intent.putExtra("currentTitle", current.title);
            intent.putExtra("currentArtist", current.artist);
            intent.putExtra("isPlaying", isPlaying());
            if (mediaPlayer != null && isPrepared) {
                 intent.putExtra("duration", mediaPlayer.getDuration());
            }
        }
        sendBroadcast(intent);
    }

    private void updateUI(boolean isPlaying) {
        updatePlaybackState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
        
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(isPlaying));
        }

        if (mediaPlayer != null && isPrepared) {
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration())
                    .build());
        }
    }

    private void updatePlaybackState(int state) {
        long position = (mediaPlayer != null && isPrepared) ? mediaPlayer.getCurrentPosition() : 0;
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | 
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | 
                        PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(state, position, 1.0f);
        mediaSession.setPlaybackState(builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Media Controls");
            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
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
                if (mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo((int) pos);
                    updateUI(mediaPlayer.isPlaying());
                }
            }
        });
        mediaSession.setActive(true);
    }

    private Notification createNotification(boolean isPlaying) {
        Intent prevIntent = new Intent(this, MusicPlayerService.class); prevIntent.setAction(ACTION_PREV);
        PendingIntent pPrev = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicPlayerService.class); nextIntent.setAction(ACTION_NEXT);
        PendingIntent pNext = PendingIntent.getService(this, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MusicPlayerService.class); playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_RESUME);
        PendingIntent pPlayPause = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .setStyle(mediaStyle)
                .addAction(android.R.drawable.ic_media_previous, "Prev", pPrev)
                .addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, isPlaying ? "Pause" : "Play", pPlayPause)
                .addAction(android.R.drawable.ic_media_next, "Next", pNext)
                .build();
    }
    
    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) return mediaPlayer.getCurrentPosition();
        return 0;
    }
    
    public int getDuration() {
        if (mediaPlayer != null && isPrepared) return mediaPlayer.getDuration();
        return 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }
    
    @Override
    public IBinder onBind(Intent intent) { return null; }
    
    @Override
    public void onDestroy() {
        instance = null;
        releaseWakeLock();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}