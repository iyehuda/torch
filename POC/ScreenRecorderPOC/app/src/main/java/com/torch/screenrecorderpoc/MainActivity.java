package com.torch.screenrecorderpoc;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.torch.service.ScreenRecorderService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    private boolean mediaProjectionPermissionGranted = false;
    private boolean storagePermissionGranted = false;
    private Button btnStop;
    private MyBroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize components
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new StopButtonOnClick());
        Log.v(TAG, "onCreate:");
        setupRecording();
        if(mReceiver == null) mReceiver = new MyBroadcastReceiver();
    }

    private class StopButtonOnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            destroy();
        }
    }

    protected void destroy() {
        Log.v(TAG, "destroy:");

        // Stop recording
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_STOP);
        startService(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        Log.v(TAG, "onActivityResult:");

        if(REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            // Check for permission response
            if(Activity.RESULT_OK != responseCode)
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            else {
                startScreenRecorder(responseCode, data);
                mediaProjectionPermissionGranted = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        storagePermissionGranted = (grantResult[0] == PackageManager.PERMISSION_GRANTED);
        if(!storagePermissionGranted) {
            exitDuePermission();
        }
        setupRecording();
    }

    private boolean getStoragePermission() {
        if(storagePermissionGranted) return true;
        if(Build.VERSION.SDK_INT < 23) return true;

        storagePermissionGranted = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if(storagePermissionGranted) return true;
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        return true;
    }

    private void exitDuePermission() {
        Toast.makeText(this, "Cannot continue without permissions", Toast.LENGTH_LONG).show();
        finish();
    }

    private void setupRecording() {
        if(!mediaProjectionPermissionGranted) {
            // Get permission for screen recording
            final MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            final Intent permissionIntent = manager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
        }
    }

    private void startScreenRecorder(final int resultCode, final Intent data) {
        Log.v(TAG, "startScreenRecorder:");

        if(!(getStoragePermission() || storagePermissionGranted))
            return;

        Toast.makeText(this, "Saving in: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), Toast.LENGTH_LONG).show();


        // Start recording
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        startService(intent);
    }

    private static final class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.v(TAG, "onReceive:" + intent);
            final String action = intent.getAction();
            if (ScreenRecorderService.ACTION_DEBUG.equals(action)) {
                String tag = intent.getStringExtra(ScreenRecorderService.EXTRA_DEBUG_TAG);
                String message = intent.getStringExtra(ScreenRecorderService.EXTRA_DEBUG_MESSAGE);
                Log.v(tag, message);
            }
        }
    }
}
