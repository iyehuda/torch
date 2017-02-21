package com.magshimim.torch.recording;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;

import com.magshimim.torch.BuildConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class FrameRecorder implements IFrameRecorder {
    private final static String TAG = "FrameRecorder";
    private final static boolean DEBUG = BuildConfig.DEBUG;

    // Used in order to save reconstruction of Bitmap object
    private Bitmap latest;

    // Handler for callbacks of media projection and image reader
    private Handler handler;

    // Thread for the handler
    private HandlerThread handlerThread;

    // An object for acquiring captured bitmap
    private ImageReader imageReader;

    // A callback that is called with a captured bitmap, supplied by the user
    private IFrameCallback callback;

    // MediaProjection for recording the screen
    private MediaProjection mediaProjection;

    // An external exception handler
    private Thread.UncaughtExceptionHandler exceptionHandler;

    // VirtualDisplay for contain the recorded data
    private VirtualDisplay virtualDisplay;

    // Display metrics
    private int height, width, dpi;

    // Used to prevent re-calls of start/stop methods
    private boolean recording;

    /**
     * Construct new frame recorder.
     * @param mediaProjection A MediaProjection object for recording the screen
     * @param display The display to be recorder
     * @param dpi The display dpi
     * @param callback The callback after capturing a frame
     * @param exceptionHandler The exception handler for uncaught exceptions
     */
    public FrameRecorder(@NonNull MediaProjection mediaProjection,
                         @NonNull Display display,
                         int dpi,
                         @NonNull IFrameCallback callback,
                         @NonNull Thread.UncaughtExceptionHandler exceptionHandler) {
        if(DEBUG) Log.d(TAG, "Constructor");

        this.mediaProjection = mediaProjection;
        this.callback = callback;
        this.dpi = dpi;
        this.exceptionHandler = exceptionHandler;
        latest = null;
        recording = false;

        // Get display size
        Point size = new Point();
        display.getSize(size);
        height = size.y;
        width = size.x;
        if(DEBUG) Log.d(TAG, "Height: " + height + ", Width: " + width);

        // Create running objects
        handlerThread =
                new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_BACKGROUND);
        if(DEBUG) Log.d(TAG, "HandlerThread is created");
        handlerThread.setUncaughtExceptionHandler(this.exceptionHandler);
        if(DEBUG) Log.d(TAG, "UncaughtExceptionHandler is set");
        handler = new Handler(handlerThread.getLooper());
        if(DEBUG) Log.d(TAG, "Handler is created");
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        if(DEBUG) Log.d(TAG, "ImageReader is created");
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                FrameRecorder.this.callback.onFrameCaptured(getBitmap(reader));
            }
        }, handler);
        if(DEBUG) Log.d(TAG, "ImageReader listener is registered");
    }


    /**
     * Starts recording.
     * Nothing is done if the recording has already started.
     * The function starts a handler thread for recording.
     */
    public synchronized void startRecording() {
        if(DEBUG) Log.d(TAG, "startRecording");
        // If already recording then return
        if(recording) {
            if(DEBUG) Log.d(TAG, "already recording");
            return;
        }
        // Start relevant tasks
        handlerThread.start();
        if(DEBUG) Log.d(TAG, "HandlerThread started");
        virtualDisplay = mediaProjection.createVirtualDisplay("CurrentFrame", width, height, dpi,
                0, imageReader.getSurface(), null, handler);
        if(DEBUG) Log.d(TAG, "VirtualDisplay created");
        MediaProjection.Callback cb=new MediaProjection.Callback() {
            @Override
            public void onStop() {
                virtualDisplay.release();
            }
        };
        mediaProjection.registerCallback(cb, handler);
        if(DEBUG) Log.d(TAG, "Registered MediaProjection callback");

        // Set recording to true
        if(objectsInitialized()) {
            recording = true;
            return;
        }

        if(DEBUG) Log.d(TAG, "startRecording failed");
        stopRecording();
        if(DEBUG) Log.d(TAG, "Stopped recording");
        exceptionHandler.uncaughtException(Thread.currentThread(),
                new IllegalStateException("Objects initialization went wrong"));
    }

    /**
     * Stops recording.
     * This function stops running objects and cleans up any necessary objects.
     */
    public synchronized void stopRecording() {
        if(DEBUG) Log.d(TAG, "stopRecording");

        if(mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            if(DEBUG) Log.d(TAG, "cleaned mediaProjection");
        }
        else Log.w(TAG, "mediaProjection was null");

        if(imageReader != null) {
            imageReader.close();
            imageReader = null;
            if(DEBUG) Log.d(TAG, "cleaned imageReader");
        }
        else Log.w(TAG, "imageReader was null");

        if(virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
            if(DEBUG) Log.d(TAG, "cleaned virtualDisplay");
        }
        else Log.w(TAG, "virtualDisplay was null");

        if(handlerThread != null) {
            handlerThread.quit();
            handlerThread = null;
            if(DEBUG) Log.d(TAG, "cleaned handlerThread");
        }
        else Log.w(TAG, "handlerThread was null");

        if(handler != null) {
            handler.getLooper().quit();
            handler = null;
            if(DEBUG) Log.d(TAG, "cleaned handler");
        }
        else Log.w(TAG, "handler is null");

        if(latest != null) {
            latest.recycle();
            latest = null;
            if(DEBUG) Log.d(TAG, "cleaned latest");
        }
        else Log.w(TAG, "latest was null");

        recording = false;
    }

    /**
     * A function for extracting Bitmap from ImageReader.
     * @param reader An ImageReader
     * @return PNG compressed bitmap
     */
    @Nullable
    private Bitmap getBitmap(@NonNull ImageReader reader) {
        if(DEBUG) Log.d(TAG, "getBitmap");
        // Get latest bitmap
        final Image image = reader.acquireLatestImage();

        // Return latest bitmap in case of null result
        if(image == null) {
            if(DEBUG) Log.d(TAG, "No new image, return latest one");
            return latest;
        }

        // Convert new Image object to bitmap
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int bitmapWidth = width + rowPadding / pixelStride;

        // Create new bitmap object if the current has no matching dimensions
        if(latest == null ||
            latest.getWidth() != bitmapWidth ||
            latest.getHeight() != height) {
            if (latest != null) {
                if(DEBUG) Log.d(TAG, "Recycling latest bitmap");
                latest.recycle();
            }
            if(DEBUG) Log.d(TAG, "Creating new bitmap");
            latest = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        }

        // Load the frame data ro the bitmap object
        latest.copyPixelsFromBuffer(buffer);
        if(DEBUG) Log.d(TAG, "Loaded data to bitmap");
        image.close();
        if(DEBUG) Log.d(TAG, "Closed Image object");

        // Compress to PNG
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Bitmap cropped = Bitmap.createBitmap(latest, 0, 0, width, height);
        cropped.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] compressedBytes = byteArrayOutputStream.toByteArray();
        latest.recycle();
        latest = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);
        if(DEBUG) Log.d(TAG, "Compressed frame");
        return latest;
    }

    /**
     * Checks if all recording objects are initialized
     * @return true in case of valid state, else false
     */
    private boolean objectsInitialized() {
        if(DEBUG) Log.d(TAG, "objectsInitialized");
        boolean retVal = true;
        if(handlerThread == null) {
            Log.w(TAG, "handlerThread is null");
            retVal = false;
        }
        if(handler == null) {
            Log.w(TAG, "handler is null");
            retVal = false;
        }
        if(imageReader == null) {
            Log.w(TAG, "imageReader is null");
            retVal = false;
        }
        if(mediaProjection == null) {
            Log.w(TAG, "mediaProjection is null");
            retVal = false;
        }
        if(virtualDisplay == null) {
            Log.w(TAG, "virtualDisplay is null");
            retVal = false;
        }
        return retVal;
    }
}
