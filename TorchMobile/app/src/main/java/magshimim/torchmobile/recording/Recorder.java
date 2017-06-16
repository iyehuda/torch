package magshimim.torchmobile.recording;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;

import magshimim.torchmobile.utils.Event;

public class Recorder {
    private final static String TAG = Recorder.class.getSimpleName();

    private boolean working;
    private int dpi;
    private int screenHeight;
    private int screenWidth;

    private FpsTimer timer;
    private Handler handler;
    private HandlerThread handlerThread;
    private ImageReader imageReader;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;

    public Event<Bitmap> onFrameCaptured;
    public Event<Exception> onError;
    public Event<Void> onClosed;

    public Recorder(RecordingSettings settings) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        settings.display.getMetrics(displayMetrics);
        dpi = displayMetrics.densityDpi;
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
        handlerThread = new HandlerThread("RecorderT", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.w(TAG, "Uncaught exception from " + t.getName(), e);
            }
        });
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        onClosed = new Event<>();
        onError = new Event<>();
        onFrameCaptured = new Event<>();
        projection = settings.mediaProjection;
        timer = new FpsTimer(settings.fps);
        working = false;
    }

    public void start() {
        if(working)
            return;

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    processFrame();
                } catch (Exception e) {
                    onError.invoke(e);
                }
            }
        }, handler);

        virtualDisplay = projection.createVirtualDisplay("FrameHolder", screenWidth, screenHeight,
                dpi, 0, imageReader.getSurface(), null, handler);

        timer.set();
        working = true;
    }

    public void close() {
        if(!working)
            return;

        working = false;
        projection.stop();
        virtualDisplay.release();
        imageReader.close();
        if(handlerThread.getId() != Thread.currentThread().getId())
            handlerThread.quitSafely();
        onClosed.invoke(null);
    }

    private void processFrame() throws Exception {
        Bitmap frame = imageToBitmap(imageReader.acquireLatestImage());
        if(frame != null) onFrameCaptured.invoke(frame);
    }

    @Nullable
    private Bitmap imageToBitmap(Image image) {
        if(image == null)
            return null;

        Bitmap result = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        try {
            result.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            result.recycle();
            return null;
        }
        try {
            image.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
