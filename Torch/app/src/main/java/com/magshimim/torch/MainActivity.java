package com.magshimim.torch;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_MEDIA_PROJECTION = 1;
    private final String TAG = getClass().getSimpleName();
    private int mResultCode;
    private Intent mResultData;
    private MediaProjectionManager mediaProjectionManager;

    public boolean isAccessible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaProjectionManager = (MediaProjectionManager)
                getSystemService(getApplicationContext().MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_MEDIA_PROJECTION) {
            if(resultCode != RESULT_OK) {
                Log.i(TAG, "user cancelled");
                return;
            }
            mResultCode = requestCode;
            mResultData = data;
        }
    }
}
