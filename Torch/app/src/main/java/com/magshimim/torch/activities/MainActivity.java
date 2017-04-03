package com.magshimim.torch.activities;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.magshimim.torch.BuildConfig;
import com.magshimim.torch.R;
import com.magshimim.torch.services.RecorderService;

public class MainActivity extends AppCompatActivity {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final int REQUEST_MEDIA_PROJECTION = 1;
    private final String TAG = getClass().getSimpleName();
    private MediaProjectionManager mediaProjectionManager;
    private boolean recording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaProjectionManager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);

        final ToggleButton recordingToggleButton = (ToggleButton) findViewById(R.id.recordingToggleButton);
        recordingToggleButton.setChecked(recording);
        recordingToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    startRecording();
                else
                    stopRecording();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(TAG, "onActivityResult");
        if(requestCode == REQUEST_MEDIA_PROJECTION) {
            if(resultCode != RESULT_OK) {
                Log.i(TAG, "user cancelled");
                return;
            }
            Log.d(TAG, "result code is " + resultCode);
            Log.d(TAG, "result canceled is " + RESULT_CANCELED);

            Intent recordingIntent = new Intent(this, RecorderService.class);
            recordingIntent.putExtra(RecorderService.EXTRA_ADDRESS, "10.0.0.10");
            recordingIntent.putExtra(RecorderService.EXTRA_PORT, 27015);
            recordingIntent.putExtra(RecorderService.EXTRA_FPS, 5);
            recordingIntent.putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode);
            recordingIntent.putExtra(RecorderService.EXTRA_RESULT_DATA, data);
            String content = describeBundle(recordingIntent.getExtras());
            Log.w(TAG, "intent:\n" + content);
            startService(recordingIntent);
            recording = true;
            if(DEBUG) Log.d(TAG, "service start intent sent");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("recording", recording);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recording = savedInstanceState.getBoolean("recording", false);
    }

    /**
     * Ask user permission
     */
    private void startRecording() {
        if(DEBUG) Log.d(TAG, "startRecording");
        if(recording) {
            Log.w(TAG, "already recording");
            return;
        }
        startActivityForResult(
        mediaProjectionManager.createScreenCaptureIntent(),
        REQUEST_MEDIA_PROJECTION);
    }

    /**
     * Stop the recorder service
     */
    private void stopRecording() {
        if(DEBUG) Log.d(TAG, "stopRecording");
        Intent recordingIntent = new Intent(this, RecorderService.class);
        stopService(recordingIntent);
        recording = false;
        if(DEBUG) Log.d(TAG, "service stopped");
    }

    private String describeBundle(Bundle bundle) {
        String content = "";
        for(String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if(value != null)
                content += String.format("%s %s\n", key, value.toString());
        }
        return content;
    }
}
