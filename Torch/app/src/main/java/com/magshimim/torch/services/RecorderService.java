package com.magshimim.torch.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.magshimim.torch.BuildConfig;
import com.magshimim.torch.networking.INetworkManager;
import com.magshimim.torch.recording.IFrameRecorder;

public class RecorderService extends Service {
    private final static String TAG = "RecorderService";
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private static IFrameRecorder recorder;
    private static INetworkManager networkManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(DEBUG) Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DEBUG) Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(DEBUG) Log.d(TAG, "onDestroy");
        if(recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }
        if(networkManager != null) {
            networkManager.disconnect();
            networkManager = null;
        }
    }

    private void record(Intent data) {
        
    }
}
