package com.torch.screenrecorderpoc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.torch.service.ScreenRecorderService;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize components
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get permission for screen recording
        final MediaProjectionManager manager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final Intent permissionIntent = manager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onDestroy() {
        // Stop recording
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_STOP);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent data) {
        super.onActivityResult(requestCode, responseCode, data);
        if(REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            // Check for permission response
            if(Activity.RESULT_OK != responseCode)
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            else
                startScreenRecorder(responseCode, data);
        }
    }

    private void startScreenRecorder(final int resultCode, final Intent data) {
        // Start recording
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        startService(intent);
    }
}
