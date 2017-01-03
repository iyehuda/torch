package com.torch.screenrecorderpoc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.torch.service.ScreenRecorderService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    private boolean permissionGranted = false;
    private MyBroadcastReceiver mReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize components
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v(TAG, "onCreate:");
        if(!permissionGranted) {
            // Get permission for screen recording
            final MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            final Intent permissionIntent = manager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
        }
        if(mReceiver == null) mReceiver = new MyBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy:");

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
                permissionGranted = true;
            }
        }
    }

    private void startScreenRecorder(final int resultCode, final Intent data) {
        Log.v(TAG, "startScreenRecorder:");

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
