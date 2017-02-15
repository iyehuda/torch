package com.magshimim.torch.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.magshimim.torch.BuildConfig;
import com.magshimim.torch.networking.INetworkManager;
import com.magshimim.torch.recording.FrameRecorder;
import com.magshimim.torch.recording.IFrameRecorder;

public class RecorderService extends Service {
    // Debug variables
    private final static String TAG = "RecorderService";
    private final static boolean DEBUG = BuildConfig.DEBUG;

    // Inner components
    private static IFrameRecorder recorder;
    private static INetworkManager networkManager;

    // Intent parameter entries
    public final static String EXTRA_ADDRESS = "address";
    public final static String EXTRA_PORT = "port";
    public final static String EXTRA_RESULT_CODE = "resultCode";
    public final static String EXTRA_RESULT_DATA = "resultData";

    // Parameters from the caller
    String address;
    int port;
    int mediaProjectionResultCode;
    Intent mediaProjectionResultData;

    // etc
    boolean working = false;
    MediaProjectionManager projectionManager;
    Thread starterThread;
    WindowManager windowManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(DEBUG) Log.d(TAG, "onCreate");
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DEBUG) Log.d(TAG, "onStartCommand");

        address = intent.getStringExtra(EXTRA_ADDRESS);
        port = intent.getIntExtra(EXTRA_PORT, -1);
        mediaProjectionResultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
        mediaProjectionResultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if(invalidParameters())
            stopSelf();
        starterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
        starterThread.start();
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

    private boolean invalidParameters() {
        if(address == null || address.equals("")) {
            Log.e(TAG, "No address received");
            return true;
        }
        if(port == -1) {
            Log.e(TAG, "No port received");
            return true;
        }
        if(mediaProjectionResultCode == -1) {
            Log.e(TAG, "No result code received");
            return true;
        }
        if(mediaProjectionResultData == null) {
            Log.e(TAG, "No result data received");
        }
        return false;
    }

    private synchronized void start() {
        if(DEBUG) Log.d(TAG, "start");
        if(working)
            return;

        // Get FrameRecorder parameters data
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int dpi = metrics.densityDpi;
        MediaProjection projection = projectionManager.getMediaProjection(
                mediaProjectionResultCode, mediaProjectionResultData);

        // Construct the inner components
        // TODO: uncomment the following when network manager is ready
        // networkManager = new NetworkManager();
        recorder = new FrameRecorder(projection, display, dpi, new IFrameRecorder.IFrameCallback() {
            @Override
            public void onFrameCaptured(Bitmap frame) {
                callback(frame);
            }
        });
        try {
            // Start communication and recording
            // TODO: uncomment the following when network manager is ready
            // networkManager.connect(address, port);
            recorder.stopRecording();
        } catch (Exception e) {
            // Break on exception
            Log.e(TAG, "Error while start recording", e);
            stopSelf();
        }
        working = true;
    }

    private void callback(Bitmap frame) {
        if(DEBUG) Log.d(TAG, "callback");
        // Pass the frame to the network manager
        // TODO: uncomment the following when network manager is ready
        // networkManager.sendFrame(frame);
    }
}
