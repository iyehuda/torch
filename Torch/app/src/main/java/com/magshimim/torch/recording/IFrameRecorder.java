package com.magshimim.torch.recording;

import android.graphics.Bitmap;

public interface IFrameRecorder {
    void startRecording();
    void stopRecording();

    interface IFrameCallback {
        void onFrameCaptured(Bitmap frame);
    }
}
