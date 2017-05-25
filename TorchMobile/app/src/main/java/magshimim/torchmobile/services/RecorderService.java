package magshimim.torchmobile.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;

import magshimim.torchmobile.networking.INetworkManager;
import magshimim.torchmobile.networking.MirroringNetworkManager;
import magshimim.torchmobile.recording.FrameRecorder;


public class RecorderService extends Service {
    private final static String TAG = "RecorderService";

    public final static String EXTRA_ADDRESS = "address";
    public final static String EXTRA_FPS = "fps";
    public final static String EXTRA_PORT = "port";
    public final static String EXTRA_RESULT_CODE = "resultCode";
    public final static String EXTRA_RESULT_DATA = "resultData";

    private static FrameRecorder recorder;
    private static INetworkManager networkManager;
    private static MediaProjectionManager projectionManager;
    private static Thread starterThread;
    private static WindowManager windowManager;

    private static boolean working, handlingException;

    private String address;
    private int fps, port, resultCode;
    private Intent resultData;

    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e(TAG, "Error from thread " + t.getName(), e);
            if(handlingException)
                return;

            handlingException = true;
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        working = false;
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Hello from the other side");
        if(working)
            return START_NOT_STICKY;

        address = intent.getStringExtra(EXTRA_ADDRESS);
        port = intent.getIntExtra(EXTRA_PORT, -1);
        resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        fps = intent.getIntExtra(EXTRA_FPS, -1);

        if(address == null || address.equals("") ||
                port <= 0 || port >= 65535 ||
                resultCode != -1 ||
                resultData == null)
            stopSelf();

        starterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
        starterThread.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        cleanup();
    }

    private void start() {
        if(working)
            return;

        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int dpi = metrics.densityDpi;
        MediaProjection projection = projectionManager.getMediaProjection(resultCode, resultData);

        networkManager = new MirroringNetworkManager();
        recorder = new FrameRecorder(projection, display, dpi, fps, new FrameRecorder.IFrameCallback() {
            @Override
            public void onFrameCaptured(Bitmap frame) {
                callback(frame);
            }
        }, new ExceptionHandler());

        try {
            networkManager.connect(address, port);
            recorder.startRecording();
            working = true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot start recording", e);
            stopSelf();
        }
    }

    private byte[] compressBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void callback(Bitmap frame) {
        if(!working || frame == null || frame.isRecycled())
            return;
        byte[] data = compressBitmap(frame);
        try {
            networkManager.sendData(data);
        } catch (Exception e) {
            if(working)
                Log.e(TAG, "Could not add data to the sending queue", e);
        }
    }

    private synchronized void cleanup() {
        if(recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }

        if(networkManager != null) {
            networkManager.disconnect();
            networkManager = null;
        }

        if(starterThread != null) {
            if(starterThread.isAlive())
                starterThread.interrupt();
            starterThread = null;
        }

        working = false;
    }
}
