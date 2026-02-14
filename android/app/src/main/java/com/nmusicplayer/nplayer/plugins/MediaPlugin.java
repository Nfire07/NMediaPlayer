package com.nmusicplayer.nplayer.plugins;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.nmusicplayer.nplayer.MusicPlayerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private final BroadcastReceiver mediaActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            
            if ("com.nmusicplayer.TRACK_CHANGE".equals(intentAction)) {
                JSObject ret = new JSObject();
                ret.put("action", "songChanged");
                ret.put("index", intent.getIntExtra("index", -1));
                ret.put("title", intent.getStringExtra("title"));
                ret.put("artist", intent.getStringExtra("artist"));
                ret.put("isPlaying", intent.getBooleanExtra("isPlaying", true));
                notifyListeners("playerStateChange", ret);
                return;
            }

            String customAction = intent.getStringExtra("action");
            if (customAction != null) {
                JSObject ret = new JSObject();
                ret.put("action", customAction);

                if (intent.hasExtra("index")) {
                    ret.put("index", intent.getIntExtra("index", -1));
                } else if (intent.hasExtra("currentIndex")) {
                    ret.put("index", intent.getIntExtra("currentIndex", -1));
                }
                
                if (intent.hasExtra("isPlaying")) {
                    ret.put("isPlaying", intent.getBooleanExtra("isPlaying", false));
                }
                
                if (intent.hasExtra("duration")) {
                     ret.put("duration", intent.getIntExtra("duration", 0));
                }
                
                notifyListeners("playerStateChange", ret);
                notifyListeners("notificationAction", ret);
            }
        }
    };

    @Override
    public void load() {
        super.load();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.nmusicplayer.MEDIA_ACTION");
        filter.addAction("com.nmusicplayer.TRACK_CHANGE"); 
        
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
    }

    @PluginMethod
    public void play(PluginCall call) {
        String path = call.getString("path");
        String title = call.getString("title");
        String artist = call.getString("artist");
        Integer index = call.getInt("index", -1);

        if (path == null) {
            call.reject("Path is required");
            return;
        }

        Intent intent = new Intent(getContext(), MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_PLAY);
        intent.putExtra(MusicPlayerService.EXTRA_PATH, path);
        intent.putExtra(MusicPlayerService.EXTRA_TITLE, title);
        intent.putExtra(MusicPlayerService.EXTRA_ARTIST, artist);
        intent.putExtra(MusicPlayerService.EXTRA_INDEX, index);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getContext().startForegroundService(intent);
        } else {
            getContext().startService(intent);
        }
        
        call.resolve();
    }

    @PluginMethod
    public void pause(PluginCall call) {
        sendCommand(MusicPlayerService.ACTION_PAUSE);
        call.resolve();
    }

    @PluginMethod
    public void resume(PluginCall call) {
        sendCommand(MusicPlayerService.ACTION_RESUME);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        sendCommand(MusicPlayerService.ACTION_STOP);
        call.resolve();
    }

    @PluginMethod
    public void seek(PluginCall call) {
        Double posDouble = call.getDouble("position");
        if (posDouble == null) {
            call.reject("Invalid position");
            return;
        }
        Intent intent = new Intent(getContext(), MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_SEEK);
        intent.putExtra(MusicPlayerService.EXTRA_POSITION, posDouble.intValue());
        getContext().startService(intent);
        call.resolve();
    }

    private void sendCommand(String action) {
        Intent intent = new Intent(getContext(), MusicPlayerService.class);
        intent.setAction(action);
        getContext().startService(intent);
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

    @PermissionCallback 
    public void permissionCallback(PluginCall call) {
        if (getPermissionState("notifications") == com.getcapacitor.PermissionState.GRANTED) {
            call.resolve();
        } else {
            call.reject("Permission denied");
        }
    }

    @PluginMethod
    public void getSongs(PluginCall call) {
        ContentResolver contentResolver = getContext().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
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
                song.put("duration", cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) / 1000.0);
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
                        String existingName = file.getName().replace(".json", "").replace("_", " ").toLowerCase();
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
    public void getPlaybackStatus(PluginCall call) {
        JSObject ret = new JSObject();
        MusicPlayerService service = MusicPlayerService.getInstance();
        if (service != null) {
            double currentPosSeconds = service.getCurrentPosition() / 1000.0;
            boolean isPlaying = service.isPlaying();
            ret.put("currentTime", currentPosSeconds);
            ret.put("isPlaying", isPlaying);
            ret.put("duration", service.getDuration() / 1000.0);
        } else {
            ret.put("currentTime", 0);
            ret.put("isPlaying", false);
            ret.put("duration", 0);
        }
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
            while ((line = br.readLine()) != null) content.append(line);
            br.close();

            JSONObject playlist = new JSONObject(content.toString());
            JSONArray originalSongs = playlist.getJSONArray("songs");
            
            JSONArray validSongsJson = new JSONArray();
            boolean hasChanges = false;

            for (int i = 0; i < originalSongs.length(); i++) {
                JSONObject songObj = originalSongs.getJSONObject(i);
                String songPath = songObj.optString("path");
                
                File songFile = new File(songPath);
                
                if (songFile.exists()) {
                    songObj.put("queueId", validSongsJson.length() + 1);
                    validSongsJson.put(songObj);
                } else {
                    hasChanges = true;
                    Log.d(TAG, "Removed missing song from playlist: " + songPath);
                }
            }

            if (hasChanges) {
                playlist.put("songs", validSongsJson);
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(playlist.toString(2));
                }
            }

            JSArray songs = new JSArray();
            for (int i = 0; i < validSongsJson.length(); i++) {
                JSONObject songObj = validSongsJson.getJSONObject(i);
                JSObject song = new JSObject();
                Iterator<String> keys = songObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = songObj.get(key);
                    if (value instanceof String) song.put(key, (String) value);
                    else if (value instanceof Integer) song.put(key, (Integer) value);
                    else if (value instanceof Long) song.put(key, (Long) value);
                    else if (value instanceof Double) song.put(key, (Double) value);
                    else if (value instanceof Boolean) song.put(key, (Boolean) value);
                    else song.put(key, value.toString());
                }
                songs.put(song);
            }

            JSObject ret = new JSObject();
            ret.put("songs", songs);
            ret.put("wasCleaned", hasChanges); 
            call.resolve(ret);

        } catch (Exception e) { 
            call.reject("Failed to read playlist songs: " + e.getMessage()); 
        }
    }

    @PluginMethod
    public void updatePlaylistOrder(PluginCall call) {
        String path = call.getString("path");
        JSArray newSongs = call.getArray("songs");
        if (path == null || path.isEmpty() || newSongs == null || newSongs.length() == 0) { call.reject("Invalid path or songs"); return; }
        File file = new File(path);
        if (!file.exists()) { call.reject("Playlist file not found"); return; }
        try {
            StringBuilder content = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) content.append(line);
            br.close();
            JSONObject playlist = new JSONObject(content.toString());
            JSONArray updatedSongs = new JSONArray();
            for (int i = 0; i < newSongs.length(); i++) {
                JSONObject song = newSongs.getJSONObject(i);
                song.put("queueId", i + 1);
                updatedSongs.put(song);
            }
            playlist.put("songs", updatedSongs);
            try (FileWriter writer = new FileWriter(file)) { writer.write(playlist.toString(2)); }
            call.resolve();
        } catch (Exception e) { call.reject("Failed to update playlist order: " + e.getMessage()); }
    }

    @PluginMethod
    public void removePlaylist(PluginCall call) {
        String name = call.getString("name");
        if (name == null || name.isEmpty()) { call.reject("Invalid playlist name"); return; }
        try {
            File dir = getContext().getFilesDir();
            String filename = name.replaceAll("\\s+", "_") + ".json";
            File file = new File(dir, filename);
            if (file.exists()) {
                if (file.delete()) call.resolve();
                else call.reject("Failed to delete playlist file");
            } else call.reject("Playlist not found");
        } catch (Exception e) { call.reject("Error removing playlist: " + e.getMessage()); }
    }

    @PluginMethod
    public void getSongInfo(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) { call.reject("Invalid path"); return; }
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(path);
            mp.prepare();
            int durationMs = mp.getDuration();
            mp.release();
            JSObject result = new JSObject();
            result.put("duration", durationMs / 1000.0);
            call.resolve(result);
        } catch (Exception e) { call.reject("Failed to get song info: " + e.getMessage()); }
    }

    @PluginMethod
    public void setPlaylist(PluginCall call) {
        JSArray songsArray = call.getArray("songs");
        
        if (songsArray == null) {
            call.reject("No songs provided");
            return;
        }

        List<MusicPlayerService.SongItem> playlist = new ArrayList<>();
        try {
            List<Object> rawList = songsArray.toList();

            for (Object obj : rawList) {
                if (obj instanceof JSONObject) {
                    JSONObject json = (JSONObject) obj;
                    String path = json.optString("path", "");
                    if (path.isEmpty()) path = json.optString("url", ""); 

                    String title = json.optString("title", "Unknown Title");
                    String artist = json.optString("artist", "Unknown Artist");
                    
                    if (!path.isEmpty()) {
                        playlist.add(new MusicPlayerService.SongItem(path, title, artist));
                    }
                }
            }

            MusicPlayerService service = MusicPlayerService.getInstance();
            if (service != null) {
                service.setPlaylist(playlist);
                call.resolve();
            } else {
                call.resolve(); 
            }

        } catch (Exception e) {
            call.reject("Error parsing playlist", e);
        }
    }
    @PluginMethod
    public void removeSongFromPlaylist(PluginCall call) {
        String playlistName = call.getString("playlistName");
        String songPath = call.getString("songPath");

        if (playlistName == null || songPath == null) {
            call.reject("Playlist name and song path are required");
            return;
        }

        try {
            File dir = getContext().getFilesDir();
            String filename = playlistName.replaceAll("\\s+", "_") + ".json";
            File file = new File(dir, filename);

            if (!file.exists()) {
                call.reject("Playlist not found");
                return;
            }
            StringBuilder content = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line);
            }
            br.close();

            JSONObject playlist = new JSONObject(content.toString());
            JSONArray songs = playlist.getJSONArray("songs");
            JSONArray updatedSongs = new JSONArray();

            int newQueueId = 1;
            boolean songRemoved = false;

            for (int i = 0; i < songs.length(); i++) {
                JSONObject song = songs.getJSONObject(i);
                String currentPath = song.getString("path");

                if (currentPath.equals(songPath)) {
                    songRemoved = true;
                    continue; 
                }

                song.put("queueId", newQueueId++);
                updatedSongs.put(song);
            }

            playlist.put("songs", updatedSongs);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(playlist.toString(2));
            }

            JSObject result = new JSObject();
            result.put("success", true);
            result.put("removed", songRemoved);
            call.resolve(result);

        } catch (Exception e) {
            call.reject("Failed to remove song: " + e.getMessage());
        }
    }
    
    @PluginMethod
    public void getSongsListNotInPlaylist(PluginCall call) {
        String playlistName = call.getString("playlistName");
        if (playlistName == null) {
            call.reject("Playlist name is required");
            return;
        }

        Set<String> playlistPaths = new HashSet<>();
        try {
            File dir = getContext().getFilesDir();
            String filename = playlistName.replaceAll("\\s+", "_") + ".json";
            File file = new File(dir, filename);

            if (file.exists()) {
                StringBuilder content = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) content.append(line);
                br.close();

                JSONObject playlist = new JSONObject(content.toString());
                JSONArray songs = playlist.getJSONArray("songs");
                for (int i = 0; i < songs.length(); i++) {
                    playlistPaths.add(songs.getJSONObject(i).getString("path"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading playlist for exclusion", e);
        }

        ContentResolver contentResolver = getContext().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor cursor = contentResolver.query(uri, projection, selection, null, null);
        JSArray songs = new JSArray();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                
                if (!playlistPaths.contains(path)) {
                    JSObject song = new JSObject();
                    song.put("id", cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                    song.put("title", cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
                    song.put("artist", cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                    song.put("path", path);
                    song.put("duration", cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) / 1000.0);
                    songs.put(song);
                }
            }
            cursor.close();
        }
        JSObject ret = new JSObject();
        ret.put("songs", songs);
        call.resolve(ret);
    }

    @PluginMethod
    public void addSongToPlaylist(PluginCall call) {
        String playlistName = call.getString("playlistName");
        JSObject songData = call.getObject("song");

        if (playlistName == null || songData == null) {
            call.reject("Playlist name and song data are required");
            return;
        }

        try {
            File dir = getContext().getFilesDir();
            String filename = playlistName.replaceAll("\\s+", "_") + ".json";
            File file = new File(dir, filename);

            if (!file.exists()) {
                call.reject("Playlist not found");
                return;
            }

            StringBuilder content = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) content.append(line);
            br.close();

            JSONObject playlist = new JSONObject(content.toString());
            JSONArray songs = playlist.getJSONArray("songs");

            JSONObject newSong = new JSONObject();
            newSong.put("title", songData.getString("title"));
            newSong.put("artist", songData.optString("artist", "Unknown"));
            newSong.put("path", songData.getString("path"));
            if (songData.has("duration")) {
                newSong.put("duration", songData.getDouble("duration"));
            }
            if (songData.has("id")) {
                newSong.put("id", songData.get("id"));
            }
            
            newSong.put("queueId", songs.length() + 1);

            songs.put(newSong);
            playlist.put("songs", songs);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(playlist.toString(2));
            }

            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to add song: " + e.getMessage());
        }
    }

    @PluginMethod
    public void deleteSong(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("Path is required");
            return;
        }

        try {
            File file = new File(path);
            if (!file.exists()) {
                call.reject("File not found");
                return;
            }

            boolean deleted = file.delete();

            if (deleted) {
                Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media.DATA + "=?";
                String[] selectionArgs = new String[]{ path };
                
                getContext().getContentResolver().delete(contentUri, selection, selectionArgs);

                call.resolve();
            } else {
                call.reject("Could not delete file. Check permissions.");
            }
        } catch (Exception e) {
            call.reject("Error deleting song: " + e.getMessage());
        }
    }

    @PluginMethod
    public void renameSong(PluginCall call) {
        String path = call.getString("path");
        String newName = call.getString("newName");

        if (path == null || newName == null || newName.isEmpty()) {
            call.reject("Path and new name are required");
            return;
        }

        String extension = "";
        int i = path.lastIndexOf('.');
        if (i > 0) {
            extension = path.substring(i);
        }

        String finalFileName = newName + extension;
        Context context = getContext();
        ContentResolver resolver = context.getContentResolver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media.DATA + "=?";
                String[] selectionArgs = new String[]{ path };

                Long id = null;
                try (Cursor cursor = resolver.query(contentUri, new String[]{MediaStore.Audio.Media._ID}, selection, selectionArgs, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    }
                }

                if (id == null) {
                    call.reject("Song not found in MediaStore");
                    return;
                }

                Uri itemUri = ContentUris.withAppendedId(contentUri, id);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, finalFileName);

                int rowsUpdated = resolver.update(itemUri, values, null, null);

                if (rowsUpdated > 0) {
                    String newPath = null;
                    try (Cursor cursor = resolver.query(itemUri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            newPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                        }
                    }

                    JSObject result = new JSObject();
                    result.put("success", true);
                    result.put("newPath", newPath != null ? newPath : path);
                    call.resolve(result);
                } else {
                    call.reject("Failed to rename file via MediaStore");
                }

            } catch (Exception e) {
                call.reject("Error renaming song: " + e.getMessage());
            }
        } else {
            File oldFile = new File(path);
            if (!oldFile.exists()) {
                call.reject("File not found");
                return;
            }

            File newFile = new File(oldFile.getParent(), finalFileName);
            if (newFile.exists()) {
                call.reject("A file with this name already exists");
                return;
            }

            boolean renamed = oldFile.renameTo(newFile);

            if (renamed) {
                try {
                    resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Audio.Media.DATA + "=?",
                            new String[]{ path });

                    MediaScannerConnection.scanFile(context,
                            new String[]{ newFile.getAbsolutePath() },
                            null,
                            null);
                } catch (Exception e) {}

                JSObject result = new JSObject();
                result.put("success", true);
                result.put("newPath", newFile.getAbsolutePath());
                call.resolve(result);
            } else {
                call.reject("Failed to rename file");
            }
        }
    }
}