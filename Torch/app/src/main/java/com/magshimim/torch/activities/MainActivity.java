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

    /**
     * Called when the activity launched to the screen
     * Sets UI components
     * @param savedInstanceState Not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(DEBUG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get media projection system service
        mediaProjectionManager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);

        // Get the instance of the toggle button
        final ToggleButton recordingToggleButton = (ToggleButton) findViewById(R.id.recordingToggleButton);

        recordingToggleButton.setChecked(recording);

        // turn on -> start recording; turn off -> stopRecording
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

    /**
     * Called when the activity gets result to request
     * @param requestCode The request code
     * @param resultCode The result code
     * @param data The data that was sent with the request
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(TAG, "onActivityResult");
        // Handle media projection response
        if(requestCode == REQUEST_MEDIA_PROJECTION) {
            // Handle user cancel
            if(resultCode != RESULT_OK) {
                Log.i(TAG, "user cancelled");
                return;
            }
            Log.d(TAG, "result code is " + resultCode);
            Log.d(TAG, "result canceled is " + RESULT_CANCELED);

            // Create an intent for the recording service
            Intent recordingIntent = new Intent(this, RecorderService.class);
            recordingIntent.putExtra(RecorderService.EXTRA_ADDRESS, "10.0.0.32"); // Server IP
            recordingIntent.putExtra(RecorderService.EXTRA_PORT, 27015); // Server port
            recordingIntent.putExtra(RecorderService.EXTRA_FPS, 30); // Frames per second
            recordingIntent.putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode); // Result code
            recordingIntent.putExtra(RecorderService.EXTRA_RESULT_DATA, data); // Result data
            startService(recordingIntent); // Start the recording service
            recording = true; // Set recording to true
            if(DEBUG) Log.d(TAG, "service start intent sent");
        }
    }

    /**
     * Called when the activity gets off the UI
     * Saves necessary data to the future
     * @param outState A bundle to write the data
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("recording", recording); // Write recording field
    }

    /**
     * Called when the activity returns to the UI (before onCreate)
     * Restores necessary data from the previous save
     * @param savedInstanceState A bundle with saved data
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recording = savedInstanceState.getBoolean("recording", false); // Restore recording field
    }

    /**
     * Asks for recording user permission
     */
    private void startRecording() {
        if(DEBUG) Log.d(TAG, "startRecording");
        if(recording) {
            Log.w(TAG, "already recording");
            return;
        }
        // Ask for permission
        // User result will call onActivityResult
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION);
    }

    /**
     * Stop the recorder service
     */
    private void stopRecording() {
        if(DEBUG) Log.d(TAG, "stopRecording");
        if(!recording) {
            Log.w(TAG, "Not recording");
            return;
        }
        Intent recordingIntent = new Intent(this, RecorderService.class);
        stopService(recordingIntent);
        recording = false;
        if(DEBUG) Log.d(TAG, "service stopped");
    }
}
