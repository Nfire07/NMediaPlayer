package com.nmediaplayer.mediaplayer.plugins;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;

import com.getcapacitor.annotation.Permission;
import com.nmediaplayer.mediaplayer.MusicPlayerService;
@CapacitorPlugin(
    name = "MediaPlugin",
    permissions = {
        @Permission(
            alias = "notifications",
            strings = { Manifest.permission.POST_NOTIFICATIONS }
        )
    }
)
public class MediaPlugin extends Plugin {
    private static final String TAG = "MediaPlugin";
    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private String currentSongTitle = "";
    private String currentSongArtist = "";
    
    public static final String ACTION_PLAY = "com.nmediaplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.nmediaplayer.ACTION_PAUSE";
    public static final String ACTION_NEXT = "com.nmediaplayer.ACTION_NEXT";
    public static final String ACTION_PREV = "com.nmediaplayer.ACTION_PREV";
    public static final String ACTION_STOP = "com.nmediaplayer.ACTION_STOP";
    
    private BroadcastReceiver mediaActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action != null) {
                JSObject ret = new JSObject();
                ret.put("action", action);
                notifyListeners("notificationAction", ret);
            }
        }
    };

    @Override
    public void load() {
        super.load();
        IntentFilter filter = new IntentFilter("com.nmediaplayer.MEDIA_ACTION");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(mediaActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(mediaActionReceiver, filter);
        }
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        try {
            getContext().unregisterReceiver(mediaActionReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @PluginMethod
    public void play(PluginCall call) {
        String path = call.getString("path");
        String title = call.getString("title", "Unknown");
        String artist = call.getString("artist", "Unknown Artist");
        
        if (path == null || path.isEmpty()) {
            call.reject("Invalid path");
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            
            mediaPlayer.setOnCompletionListener(mp -> {
                JSObject ret = new JSObject();
                ret.put("action", ACTION_NEXT);
                notifyListeners("notificationAction", ret);
            });

            mediaPlayer.prepare();
            mediaPlayer.setWakeMode(getContext(), android.os.PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.start();

            isPaused = false;
            currentSongTitle = title;
            currentSongArtist = artist;
            
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to play audio: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isPaused = false;
            currentSongTitle = "";
            currentSongArtist = "";
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to stop audio: " + e.getMessage());
        }
    }

    @PluginMethod
    public void pause(PluginCall call) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPaused = true;
                call.resolve();
            } else {
                call.reject("No audio is playing");
            }
        } catch (Exception e) {
            call.reject("Pause failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void resume(PluginCall call) {
        try {
            if (mediaPlayer == null) {
                call.reject("No media player initialized");
                return;
            }
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                isPaused = false;
                call.resolve();
            } else {
                call.resolve();
            }
        } catch (Exception e) {
            call.reject("Resume failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void seek(PluginCall call) {
        Integer positionSec = call.getInt("position");
        if (mediaPlayer == null || positionSec == null) {
            call.reject("Invalid media player or position");
            return;
        }

        int durationMs = mediaPlayer.getDuration();
        int positionMs = positionSec * 1000;
        if (positionMs < 0 || positionMs > durationMs) {
            call.reject("Position out of range");
            return;
        }

        try {
            mediaPlayer.seekTo(positionMs);
            call.resolve();
        } catch (Exception e) {
            call.reject("Seek failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (getPermissionState("notifications") != com.getcapacitor.PermissionState.GRANTED) {
                requestPermissionForAlias("notifications", call, "permissionCallback");
            } else {
                call.resolve();
            }
        } else {
            call.resolve();
        }
    }

    @PluginMethod
    public void permissionCallback(PluginCall call) {
        if (getPermissionState("notifications") == com.getcapacitor.PermissionState.GRANTED) {
            call.resolve();
        } else {
            call.reject("Permission denied");
        }
    }

    @PluginMethod
    public void startMusicNotification(PluginCall call) {
        String title = call.getString("title", "No song playing");
        String artist = call.getString("artist", "Unknown Artist");
        Boolean isPlaying = call.getBoolean("isPlaying", true);
        
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Manca il permesso POST_NOTIFICATIONS! La notifica non apparirà.");
            }
        }

        try {
            Context context = getContext();
            Intent serviceIntent = new Intent(context, MusicPlayerService.class);
            serviceIntent.setAction(MusicPlayerService.ACTION_START_FOREGROUND);
            serviceIntent.putExtra(MusicPlayerService.EXTRA_TITLE, title);
            serviceIntent.putExtra(MusicPlayerService.EXTRA_ARTIST, artist);
            serviceIntent.putExtra(MusicPlayerService.EXTRA_IS_PLAYING, isPlaying);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to start notification: " + e.getMessage());
        }
    }

    @PluginMethod
    public void updateMusicNotification(PluginCall call) {
        String title = call.getString("title");
        String artist = call.getString("artist");
        Boolean isPlaying = call.getBoolean("isPlaying");
        
        if (title != null) currentSongTitle = title;
        if (artist != null) currentSongArtist = artist;
        
        try {
            Context context = getContext();
            Intent serviceIntent = new Intent(context, MusicPlayerService.class);
            serviceIntent.setAction(MusicPlayerService.ACTION_UPDATE_NOTIFICATION);
            serviceIntent.putExtra(MusicPlayerService.EXTRA_TITLE, currentSongTitle);
            serviceIntent.putExtra(MusicPlayerService.EXTRA_ARTIST, currentSongArtist);
            if (isPlaying != null) {
                serviceIntent.putExtra(MusicPlayerService.EXTRA_IS_PLAYING, isPlaying);
            }
            
            context.startService(serviceIntent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to update notification: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopMusicNotification(PluginCall call) {
        try {
            Context context = getContext();
            Intent serviceIntent = new Intent(context, MusicPlayerService.class);
            serviceIntent.setAction(MusicPlayerService.ACTION_STOP_FOREGROUND);
            context.startService(serviceIntent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to stop notification: " + e.getMessage());
        }
    }

    @PluginMethod
    public void updateNotification(PluginCall call) {
        updateMusicNotification(call);
    }

    @PluginMethod
    public void handleNotificationAction(PluginCall call) {
        String action = call.getString("action");
        if (action == null) {
            call.reject("No action provided");
            return;
        }

        JSObject result = new JSObject();
        result.put("action", action);
        notifyListeners("notificationAction", result);
        
        call.resolve(result);
    }

    @PluginMethod
    public void getSongs(PluginCall call) {
        ContentResolver contentResolver = getContext().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = contentResolver.query(uri, projection, selection, null, null);
        JSArray songs = new JSArray();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                JSObject song = new JSObject();
                song.put("id", cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                song.put("title", cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
                song.put("artist", cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                song.put("path", cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                songs.put(song);
            }
            cursor.close();
        }

        JSObject ret = new JSObject();
        ret.put("songs", songs);
        call.resolve(ret);
    }

    @PluginMethod
    public void createPlaylist(PluginCall call) {
        String name = call.getString("name");
        JSArray songs = call.getArray("songs");

        if (name == null || name.isEmpty() || songs == null || songs.length() == 0) {
            call.reject("Invalid name or songs");
            return;
        }

        try {
            File dir = getContext().getFilesDir();
            File[] files = dir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        String existingName = file.getName()
                                .replace(".json", "")
                                .replace("_", " ")
                                .toLowerCase();

                        if (existingName.equals(name.toLowerCase())) {
                            call.reject("A playlist with this name already exists");
                            return;
                        }
                    }
                }
            }

            JSONObject playlist = new JSONObject();
            playlist.put("name", name);
            playlist.put("createdAt", System.currentTimeMillis());
            JSONArray songList = new JSONArray();
            for (int i = 0; i < songs.length(); i++) {
                JSONObject song = songs.getJSONObject(i);
                song.put("queueId", i + 1);
                songList.put(song);
            }
            playlist.put("songs", songList);

            String filename = name.replaceAll("\\s+", "_") + ".json";
            File file = new File(dir, filename);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(playlist.toString(2));
            }

            JSObject result = new JSObject();
            result.put("path", file.getAbsolutePath());
            call.resolve(result);

        } catch (Exception e) {
            call.reject("Failed to create playlist: " + e.getMessage());
        }
    }

    @PluginMethod
    public void listPlaylists(PluginCall call) {
        File dir = getContext().getFilesDir();
        File[] files = dir.listFiles();
        JSArray playlists = new JSArray();

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    JSObject playlist = new JSObject();
                    playlist.put("name", file.getName().replace(".json", "").replace("_", " "));
                    playlist.put("path", file.getAbsolutePath());
                    playlists.put(playlist);
                }
            }
        }
        JSObject ret = new JSObject();
        ret.put("playlists", playlists);
        call.resolve(ret);
    }

    @PluginMethod
    public void getPlaylistSongs(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("Invalid path");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            call.reject("Playlist file not found");
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            br.close();

            JSONObject playlist = new JSONObject(content.toString());
            JSONArray songArray = playlist.getJSONArray("songs");
            JSArray songs = new JSArray();

            for (int i = 0; i < songArray.length(); i++) {
                JSONObject songObj = songArray.getJSONObject(i);
                JSObject song = new JSObject();

                Iterator<String> keys = songObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = songObj.get(key);

                    if (value instanceof String) {
                        song.put(key, (String) value);
                    } else if (value instanceof Integer) {
                        song.put(key, (Integer) value);
                    } else if (value instanceof Long) {
                        song.put(key, (Long) value);
                    } else if (value instanceof Double) {
                        song.put(key, (Double) value);
                    } else if (value instanceof Boolean) {
                        song.put(key, (Boolean) value);
                    } else {
                        song.put(key, value.toString());
                    }
                }
                songs.put(song);
            }

            JSObject ret = new JSObject();
            ret.put("songs", songs);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to read playlist songs: " + e.getMessage());
        }
    }

    @PluginMethod
    public void updatePlaylistOrder(PluginCall call) {
        String path = call.getString("path");
        JSArray newSongs = call.getArray("songs");

        if (path == null || path.isEmpty() || newSongs == null || newSongs.length() == 0) {
            call.reject("Invalid path or songs");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            call.reject("Playlist file not found");
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            br.close();

            JSONObject playlist = new JSONObject(content.toString());
            JSONArray updatedSongs = new JSONArray();

            for (int i = 0; i < newSongs.length(); i++) {
                JSONObject song = newSongs.getJSONObject(i);
                song.put("queueId", i + 1);
                updatedSongs.put(song);
            }

            playlist.put("songs", updatedSongs);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(playlist.toString(2));
            }

            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to update playlist order: " + e.getMessage());
        }
    }

    @PluginMethod
    public void updatePlaylistQueue(PluginCall call) {
        String path = call.getString("path");
        JSArray queue = call.getArray("queue");

        if (path == null || path.isEmpty() || queue == null) {
            call.reject("Invalid path or queue");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            call.reject("Playlist file not found");
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            br.close();

            JSONObject playlist = new JSONObject(content.toString());
            JSONArray songs = playlist.getJSONArray("songs");

            JSONObject[] songMap = new JSONObject[songs.length()];
            for (int i = 0; i < songs.length(); i++) {
                JSONObject song = songs.getJSONObject(i);
                int queueId = song.optInt("queueId", i+1);
                if (queueId > 0 && queueId <= songs.length()) {
                    songMap[queueId - 1] = song;
                }
            }

            JSONArray newSongs = new JSONArray();
            for (int i = 0; i < queue.length(); i++) {
                JSONObject q = queue.getJSONObject(i);
                int newQueueId = q.getInt("queueId");
                String songPath = q.getString("path");

                JSONObject matchingSong = null;
                for (JSONObject s : songMap) {
                    if (s != null && s.optString("path").equals(songPath)) {
                        matchingSong = s;
                        break;
                    }
                }

                if (matchingSong != null) {
                    matchingSong.put("queueId", newQueueId);
                    newSongs.put(matchingSong);
                }
            }

            playlist.put("songs", newSongs);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(playlist.toString(2));
            }

            call.resolve();

        } catch (Exception e) {
            call.reject("Failed to update playlist queue: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getSongInfo(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("Invalid path");
            return;
        }
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(path);
            mp.prepare();
            int durationMs = mp.getDuration();
            mp.release();

            JSObject result = new JSObject();
            result.put("duration", durationMs / 1000.0);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Failed to get song info: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getPlaybackStatus(PluginCall call) {
        JSObject result = new JSObject();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            result.put("isPlaying", true);
            result.put("currentTime", mediaPlayer.getCurrentPosition() / 1000.0);
        } else if (mediaPlayer != null && isPaused) {
            result.put("isPlaying", false);
            result.put("currentTime", mediaPlayer.getCurrentPosition() / 1000.0);
        } else {
            result.put("isPlaying", false);
            result.put("currentTime", 0);
        }
        call.resolve(result);
    }

    @PluginMethod
    public void removePlaylist(PluginCall call) {
        String name = call.getString("name");
        if (name == null || name.isEmpty()) {
            call.reject("Invalid playlist name");
            return;
        }

        try {
            File dir = getContext().getFilesDir();
            String filename = name.replaceAll("\\s+", "_") + ".json";
            File file = new File(dir, filename);

            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    call.resolve();
                } else {
                    call.reject("Failed to delete playlist");
                }
            } else {
                call.reject("Playlist not found");
            }

        } catch (Exception e) {
            call.reject("Error removing playlist: " + e.getMessage());
        }
    }
}