package com.nmediaplayer.mediaplayer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import android.os.Environment;
import com.getcapacitor.BridgeActivity;
import com.nmediaplayer.mediaplayer.plugins.MediaPlugin;


public class MainActivity extends BridgeActivity {
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 2296;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(MediaPlugin.class);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
                Toast.makeText(this, "Please grant all files access permission.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
