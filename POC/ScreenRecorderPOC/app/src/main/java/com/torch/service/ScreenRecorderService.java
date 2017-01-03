package com.torch.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.torch.media.*;

import java.io.IOException;

public class ScreenRecorderService extends IntentService {

    private static final String TAG = "ScreenRecorderService";

    private static final String BASE = "com.torch.service.ScreenRecorderService.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_PAUSE = BASE + "ACTION_PAUSE";
    public static final String ACTION_RESUME = BASE + "ACTION_RESUME";
    public static final String ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS";
    public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";
    public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";

    private static final Object sSync = new Object();

    private MediaMuxerWrapper mediaMuxerWrapper;
    private MediaProjectionManager mediaProjectionManager;

    public ScreenRecorderService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate:");
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onHandleIntent(final Intent intent) {
        Log.v(TAG, "onHandleIntent:");

        final String action = intent.getAction();

        switch (action) {
            case ACTION_START:
                startScreenRecord(intent);
                updateStatus();
                break;
            case ACTION_STOP:
                stopScreenRecord();
                updateStatus();
                break;
            case ACTION_QUERY_STATUS:
                updateStatus();
                break;
            case ACTION_PAUSE:
                pauseScreenRecord();
                break;
            case ACTION_RESUME:
                resumeScreenRecord();
                break;
            default:
                break;
        }
    }

    private void startScreenRecord(final Intent intent) {
        Log.v(TAG, "startScreenRecord:");

        final DisplayMetrics metrics;
        final MediaProjection mediaProjection;

        final int resultCode;
        final int density;
        synchronized (sSync) {
            if(mediaMuxerWrapper != null)
                return;

            resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intent);
            if(mediaProjection == null)
                return;

            metrics = getResources().getDisplayMetrics();
            density = metrics.densityDpi;

            try {
                mediaMuxerWrapper = new MediaMuxerWrapper(".mp4");
                new MediaScreenEncoder(mediaMuxerWrapper, mMediaEncoderListener, mediaProjection,
                        metrics.widthPixels, metrics.heightPixels, density);
                new MediaAudioEncoder(mediaMuxerWrapper, mMediaEncoderListener);
                mediaMuxerWrapper.prepare();
                mediaMuxerWrapper.startRecording();
            } catch (IOException e) {
                Log.e(TAG, "startScreenRecord: " + e);
            }
        }
    }

    private void stopScreenRecord() {
        Log.v(TAG, "stopScreenRecord:");

        synchronized (sSync) {
            if(mediaMuxerWrapper == null)
                return;

            mediaMuxerWrapper.stopRecording();
            mediaMuxerWrapper = null;
        }
    }

    private void pauseScreenRecord() {
        synchronized (sSync) {
            if(mediaMuxerWrapper != null)
                mediaMuxerWrapper.pauseRecording();
        }
    }

    private void resumeScreenRecord() {
        synchronized (sSync) {
            if(mediaMuxerWrapper != null)
                mediaMuxerWrapper.resumeRecording();
        }
    }

    private void updateStatus() {
        Log.v(TAG, "updateStatus:");

        final boolean isRecording, isPausing;
        synchronized (sSync) {
            isRecording = mediaMuxerWrapper != null;
            isPausing = isRecording && mediaMuxerWrapper.isPaused();
        }

        final Intent result = new Intent();
        result.setAction(ACTION_QUERY_STATUS_RESULT);
        result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording);
        result.putExtra(EXTRA_QUERY_RESULT_PAUSING, isPausing);
        sendBroadcast(result);
    }

    private static final MediaEncoder.MediaEncoderListener mMediaEncoderListener =
            new MediaEncoder.MediaEncoderListener() {
                static final String TAG = "MediaEncoderListener";

                @Override
                public void onPrepared(MediaEncoder encoder) {
                    Log.v(TAG, "onPrepared:");
                }

                @Override
                public void onStopped(MediaEncoder encoder) {
                    Log.v(TAG, "onStopped:");
                }
            };
}
