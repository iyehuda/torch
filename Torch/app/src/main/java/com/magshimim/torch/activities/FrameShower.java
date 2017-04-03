package com.magshimim.torch.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.magshimim.torch.BuildConfig;
import com.magshimim.torch.R;
import com.magshimim.torch.services.RecorderService;

public class FrameShower extends AppCompatActivity {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "FrameShower";

    private FrameReceiver frameReceiver = new FrameReceiver();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.d(TAG, " onCreate");
        setContentView(R.layout.activity_frame_shower);
        parseIntent(getIntent());
    }

    protected void onResume() {
        super.onResume();
        if(DEBUG) Log.d(TAG, "onResume");
        IntentFilter filter = new IntentFilter(RecorderService.SEND_FRAME_ACTION);
        registerReceiver(frameReceiver, filter);
    }

    protected void onPause() {
        super.onPause();
        if(DEBUG) Log.d(TAG, "onPause");
        unregisterReceiver(frameReceiver);
    }

    private void parseIntent(Intent intent) {
        if(DEBUG) Log.d(TAG, "parseIntent:");
        byte[] bytes = intent.getByteArrayExtra(RecorderService.EXTRA_FRAME);
        if(bytes == null) {
            Log.w(TAG, "intent does not contain frame");
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        setBitmap(bitmap);
    }

    private void setBitmap(Bitmap bitmap) {
        if(DEBUG) Log.d(TAG, "setBitmap:");
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap);
    }

    class FrameReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(DEBUG) Log.d(TAG, "onReceive");
            parseIntent(intent);
        }
    }
}
