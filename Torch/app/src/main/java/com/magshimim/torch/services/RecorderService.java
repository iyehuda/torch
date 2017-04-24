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
import java.io.ByteArrayOutputStream;

import com.magshimim.torch.BuildConfig;
import com.magshimim.torch.networking.INetworkManager;
import com.magshimim.torch.networking.NetworkManager;
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
    public final static String EXTRA_FPS = "fps";
    public final static String EXTRA_PORT = "port";
    public final static String EXTRA_RESULT_CODE = "resultCode";
    public final static String EXTRA_RESULT_DATA = "resultData";

    // Parameters from the caller
    String address;
    int fps;
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

    /**
     * Gets system services
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");
        handlingException = false;
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    /**
     * The entry point after startService is called
     * Get parameters from the service starter
     * Break if not all parameters are valid
     * Start a thread to set up environment fro recording
     *
     * @param intent  The intent used for starting this service
     * @param flags   Not used
     * @param startId Not used
     * @return START_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand");

        // Break if already working
        if (working) return START_NOT_STICKY;

        // Get connection parameters from the intent
        address = intent.getStringExtra(EXTRA_ADDRESS);
        port = intent.getIntExtra(EXTRA_PORT, -1);

        // Get media projection data from the intent
        mediaProjectionResultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        mediaProjectionResultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);

        // Get recording data
        fps = intent.getIntExtra(EXTRA_FPS, 0);

        // Validate the parameters
        if (invalidParameters())
            stopSelf();

        // Start a thread to initialize recording
        starterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
        starterThread.start();
        return START_NOT_STICKY;
    }

    /**
     * The exit point after stopService or stopSelf is called
     * Cleanup any resource being used
     */
    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        cleanup();
    }

    /**
     * Check for all service parameters validity
     *
     * @return true - All parameters are valid; false - Not all parameters are valid
     */
    private boolean invalidParameters() {
        if (DEBUG) Log.d(TAG, "invalidParameters");
        if (address == null || address.equals("")) {
            Log.e(TAG, "No address received");
            return true;
        }
        if (port == -1) {
            Log.e(TAG, "No port received");
            return true;
        }
        if (mediaProjectionResultCode == 0) {
            Log.e(TAG, "No result code received");
            return true;
        }
        if (mediaProjectionResultData == null) {
            Log.e(TAG, "No result data received");
            return true;
        }
        if (fps <= 0) {
            Log.e(TAG, "No FPS/Invalid FPS received");
            return true;
        }
        return false;
    }

    /**
     * The recording starting point
     */
    private synchronized void start() {
        if (DEBUG) Log.d(TAG, "start");
        if (working)
            return;

        // Create network manager component
        networkManager = new NetworkManager();

        // Get FrameRecorder parameters data
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int dpi = metrics.densityDpi;
        MediaProjection projection = projectionManager.getMediaProjection(
                mediaProjectionResultCode, mediaProjectionResultData);

        // Construct the inner component
        recorder = new FrameRecorder(projection, display, dpi, fps, new IFrameRecorder.IFrameCallback() {
            @Override
            public void onFrameCaptured(Bitmap frame) {
                callback(frame);
            }
        }, new ExceptionHandler());
        try {
            // Start communication and recording
            networkManager.connect(address, port);
            recorder.startRecording();
        } catch (Exception e) {
            // Break on exception
            Log.e(TAG, "Error while start recording", e);
            stopSelf();
        }
        working = true;
    }

    /**
     * Compresses a bitmap
     * @param bitmap A bitmap to compress
     * @return A byte array with jpeg format image
     */
    private byte[] compressBitmap(Bitmap bitmap) {
        // Compress to JPEG
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream);
        byte[] compressBytes = byteArrayOutputStream.toByteArray();
        if (DEBUG) Log.d(TAG, "Compressed frame");
        return compressBytes;
    }

    /**
     * The function that is called upon frame record
     *
     * @param frame The recorded frame, can be null
     */
    private void callback(@Nullable Bitmap frame) {
        if (DEBUG) Log.d(TAG, "callback:");
        if(!working) {
            Log.w(TAG, "working is off");
            return;
        }
        if(frame == null) {
            Log.w(TAG, "frame is null");
            return;
        }
        if (frame.isRecycled()) {
            Log.w(TAG, "frame is recycled");
            return;
        }
        try {
            // Pass the frame to the network manager
            long curr = System.currentTimeMillis();
            byte[] comp = compressBitmap(frame);
            long delta = System.currentTimeMillis() - curr;
            networkManager.sendData(comp);
            if(DEBUG) Log.d(TAG, "It took " + delta + " milliseconds");
        } catch (Exception e) {
            Log.e(TAG, "cannot send frame", e);
        }
    }

    /**
     * Free resources
     */
    private synchronized void cleanup() {
        if(DEBUG) Log.d(TAG, "cleanup");

        // Stop the thread
        if(starterThread != null && starterThread.isAlive()) {
            starterThread.interrupt();
            starterThread = null;
        } else Log.w(TAG, "starterThread is null or not alive");

        // Stop recording
        if(recorder != null) {
            recorder.stopRecording();
            recorder = null;
        } else Log.w(TAG, "recorder is null");

        // Stop communicating
        if(networkManager != null) {
            networkManager.disconnect();
            networkManager = null;
        } else Log.w(TAG, "networkManager is null");

        working = false;
    }

    private boolean handlingException;
    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if(DEBUG) Log.d(TAG, "uncaughtException");
            Log.e(TAG, "Exception from thread " + t.getName(), e);
            if(!handlingException) {
                handlingException = true;
                stopSelf();
            }
        }
    }
}
