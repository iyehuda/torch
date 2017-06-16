package magshimim.torchmobile.features;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import magshimim.torchmobile.networking.NetworkManager;
import magshimim.torchmobile.networking.protos.TorchMessageOuterClass;
import magshimim.torchmobile.recording.Recorder;
import magshimim.torchmobile.recording.RecordingSettings;
import magshimim.torchmobile.utils.Callback;

public class RecordingManager extends FeatureManager {
    private final static String TAG = "RecordingManager";

    private boolean working;
    private Callback<TorchMessageOuterClass.TorchMessage> messageCallback;
    private Recorder recorder;

    public RecordingManager(NetworkManager networkManager, RecordingSettings recordingSettings) {
        super(networkManager);

        messageCallback = new Callback<TorchMessageOuterClass.TorchMessage>() {
            @Override
            public void invoke(TorchMessageOuterClass.TorchMessage arg) {

            }
        };

        recorder = new Recorder(recordingSettings);

        networkManager.onMessage.registerCallback(messageCallback);
        if(networkManager.isConnected())
            start();
        else
            networkManager.onConnected.registerCallback(new Callback<Void>() {
                @Override
                public void invoke(Void arg) {
                    start();
                }
            });
    }

    @Override
    public void close() {
        if(!working)
            return;

        super.close();
        working = false;
        recorder.close();
        networkManager.onMessage.unregisterEvent(messageCallback);
    }

    @Override
    public void start() {
        if(working)
            return;

        super.start();
        working = true;
        recorder.onClosed.registerCallback(new Callback<Void>() {
            @Override
            public void invoke(Void arg) {
                working = false;
                onClosed.invoke(null);
            }
        });
        recorder.onError.registerCallback(new Callback<Exception>() {
            @Override
            public void invoke(Exception arg) {
                Log.e(TAG, "Error from recorder", arg);
            }
        });
        recorder.onFrameCaptured.registerCallback(new Callback<Bitmap>() {
            @Override
            public void invoke(Bitmap arg) {
                processFrame(arg);
            }
        });
        recorder.start();
    }

    @Override
    protected void handleMessage(TorchMessageOuterClass.TorchMessage message) {
    }

    private void processFrame(Bitmap frame) {
        CompressionTask task = new CompressionTask();
        task.execute(frame);
    }

    static {
        int processors = Runtime.getRuntime().availableProcessors();
        ((ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR)
                .setCorePoolSize(processors);
        ((ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR)
                .setMaximumPoolSize(processors * 2);
    }

    private class CompressionTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected Void doInBackground(Bitmap... params) {
            Bitmap frame = params[0];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            frame.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            frame.recycle();
            TorchMessageOuterClass.TorchMessage message = TorchMessageOuterClass.TorchMessage.newBuilder()
                    .setType(TorchMessageOuterClass.TorchMessage.MessageType.FRAME)
                    .setFrame(TorchMessageOuterClass.ByteArray.newBuilder()
                            .setData(ByteString.copyFrom(outputStream.toByteArray()))
                            .build())
                    .build();
            if(networkManager != null && networkManager.isConnected())
                try {
                    networkManager.send(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error while sending");
                }
                finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            return null;
        }
    }
}
