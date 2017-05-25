package magshimim.torchmobile;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import magshimim.torchmobile.services.RecorderService;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_MEDIA_PROJECTION = 1;
    private MediaProjectionManager mediaProjectionManager;
    private boolean recording = false;
    private String address;
    private EditText editTextAddress;
    private ToggleButton recordingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaProjectionManager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);

        editTextAddress = (EditText)findViewById(R.id.serverAddress);
        recordingButton = (ToggleButton)findViewById(R.id.recordingToggleButton);
        recordingButton.setChecked(recording);
        recordingButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    address = editTextAddress.getText().toString();
                    startRecording();
                }
                else
                    stopRecording();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != REQUEST_MEDIA_PROJECTION)
            return;

        if(resultCode == RESULT_OK) {
            Intent recordingIntent = new Intent(this, RecorderService.class);
            recordingIntent.putExtra(RecorderService.EXTRA_ADDRESS, address);
            recordingIntent.putExtra(RecorderService.EXTRA_PORT, 27015);
            recordingIntent.putExtra(RecorderService.EXTRA_FPS, 20);
            recordingIntent.putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode);
            recordingIntent.putExtra(RecorderService.EXTRA_RESULT_DATA, data);
            startService(recordingIntent);
            recording = true;
        }
        else
            recordingButton.setChecked(false);
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

    private void startRecording() {
        if(!recording)
            startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    private void stopRecording() {
        if(!recording)
            return;

        stopService(new Intent(this, RecorderService.class));
        recording = false;
    }
}
