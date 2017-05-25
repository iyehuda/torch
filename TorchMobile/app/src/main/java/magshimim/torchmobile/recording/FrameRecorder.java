package magshimim.torchmobile.recording;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class FrameRecorder {
    private final static String TAG = "FrameRecorder";

    final private Object locker;
    private Bitmap latestFrame;
    private Handler handler;
    private HandlerThread handlerThread;
    private ImageReader imageReader;
    private IFrameCallback callback;
    private MediaProjection mediaProjection;
    private Timer timer;
    private VirtualDisplay virtualDisplay;

    private boolean recording;
    private int height, width, dpi;
    private long timeDelta;

    public interface IFrameCallback {
        void onFrameCaptured(Bitmap frame);
    }

    private class FrameTask extends TimerTask {
        private void shoot() {
            Bitmap tmp = null;
            synchronized (locker) {
                if(latestFrame != null && !latestFrame.isRecycled())
                    tmp = Bitmap.createBitmap(latestFrame);
            }
            callback.onFrameCaptured(tmp);
            if(tmp != null)
                tmp.recycle();
        }

        @Override
        public void run() {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    shoot();
                    return null;
                }
            }.execute();
        }
    }

    public FrameRecorder(@NonNull MediaProjection mediaProjection,
                         @NonNull Display display,
                         int dpi,
                         int fps,
                         @NonNull IFrameCallback callback,
                         @NonNull Thread.UncaughtExceptionHandler exceptionHandler) {
        this.mediaProjection = mediaProjection;
        this.callback = callback;
        this.dpi = dpi;
        timeDelta = 1000/ fps;
        latestFrame = null;
        recording = false;
        timer = new Timer();
        locker = new Object();

        Point size = new Point();
        display.getSize(size);
        height = size.y;
        width = size.x;

        handlerThread = new HandlerThread("HandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.setUncaughtExceptionHandler(exceptionHandler);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
    }

    private void updateFrame() {
        final Image current = imageReader.acquireLatestImage();
        if(current == null)
            return;

        int pixelStride, rowStride, rowPadding, bitmapWidth;
        ByteBuffer buffer;

        try {
            Image.Plane[] planes = current.getPlanes();
            buffer = planes[0].getBuffer();
            pixelStride = planes[0].getPixelStride();
            rowStride = planes[0].getRowStride();
            rowPadding = rowStride - pixelStride * width;
            bitmapWidth = width + rowPadding / pixelStride;
        } catch (IllegalStateException e) {
            if(recording)
                Log.e(TAG, "Cannot read buffered image", e);
            return;
        }

        synchronized (locker) {
            if(latestFrame == null || latestFrame.getHeight() != height || latestFrame.getWidth() != bitmapWidth) {
                if(latestFrame != null)
                    latestFrame.recycle();
                latestFrame = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
            }
            latestFrame.copyPixelsFromBuffer(buffer);
        }

        current.close();
    }

    public void startRecording() {
        if(recording)
            return;

        virtualDisplay = mediaProjection.createVirtualDisplay("FrameHolder",
                width,
                height,
                dpi,
                0,
                imageReader.getSurface(),
                null,
                handler);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                updateFrame();
            }
        }, handler);

        recording = true;
        timer.scheduleAtFixedRate(new FrameTask(), 0, timeDelta);
    }

    public void stopRecording() {
        if(mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        if(imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if(virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if(handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }

        handler = null;

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

        synchronized (locker) {
            if(latestFrame != null && !latestFrame.isRecycled())
                latestFrame.recycle();
            latestFrame = null;
        }

        recording = false;
    }
}
