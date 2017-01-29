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
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class FrameRecorder {
    // Used in order to save reconstruction of Bitmap object
    private Bitmap latest;

    // Handler for callbacks of media projection and image reader
    private Handler handler;

    // Thread for the handler
    private HandlerThread handlerThread;

    // An object for acquiring captured bitmap
    private ImageReader imageReader;

    // A callback that is called with a captured bitmap, supplied by the user
    private OnFrameCapturedListener callback;

    // MediaProjection for recording the screen
    private MediaProjection mediaProjection;

    // VirtualDisplay for contain the recorded data
    private VirtualDisplay virtualDisplay;

    // Display metrics
    private int height, width, dpi;

    // Used to prevent re-calls of start/stop methods
    private boolean recording;

    /**
     * Construct new frame recorder
     * @param mediaProjection A MediaProjection object for recording the screen
     * @param display The display to be recorder
     * @param dpi The display dpi
     * @param callback The callback after capturing a frame
     */
    public FrameRecorder(@NonNull MediaProjection mediaProjection, @NonNull Display display,
                         int dpi,@NonNull OnFrameCapturedListener callback) {
        this.mediaProjection = mediaProjection;
        this.callback = callback;
        this.dpi = dpi;
        latest = null;
        recording = false;
        Point size = new Point();
        display.getSize(size);
        height = size.y;
        width = size.x;
    }

    public void startRecording() {
        if(recording)
            return;

        handlerThread =
                new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                callback.onFrameCaptured(getBitmap(reader));
            }
        }, handler);
        MediaProjection.Callback cb=new MediaProjection.Callback() {
            @Override
            public void onStop() {
                virtualDisplay.release();
            }
        };
        virtualDisplay = mediaProjection.createVirtualDisplay("CurrentFrame", width, height, dpi,
                0, imageReader.getSurface(), null, handler);
        mediaProjection.registerCallback(cb, handler);

        recording = true;
    }

    public void stopRecording() {
        if(!recording)
            return;

        mediaProjection.stop();
        imageReader.close();
        recording = false;
    }

    @Nullable
    private Bitmap getBitmap(ImageReader reader) {
        final Image image = reader.acquireLatestImage();
        if(image == null)
            return latest;

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        int bitmapWidth = width + rowPadding / pixelStride;

        if(latest == null || latest.getWidth() != bitmapWidth || latest.getHeight() != height) {
            if (latest != null)
                latest.recycle();

            latest = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888);
        }

        latest.copyPixelsFromBuffer(buffer);
        image.close();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Bitmap cropped = Bitmap.createBitmap(latest, 0, 0, width, height);
        cropped.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] compressedBytes = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.length);
    }

    public abstract class OnFrameCapturedListener {
        abstract void onFrameCaptured(Bitmap frame);
    }
}
