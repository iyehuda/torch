package magshimim.torchmobile.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashSet;

import magshimim.torchmobile.features.FeatureManager;
import magshimim.torchmobile.features.RecordingManager;
import magshimim.torchmobile.networking.NetworkManager;
import magshimim.torchmobile.networking.protos.TorchMessageOuterClass;
import magshimim.torchmobile.recording.RecordingSettings;
import magshimim.torchmobile.utils.Callback;
import magshimim.torchmobile.utils.Event;

import static android.app.Activity.RESULT_OK;


public class BackgroundService extends Service {
    private final static String TAG = BackgroundService.class.getSimpleName();

    private boolean working;
    private HashSet<FeatureManager> featureManagers;
    private NetworkManager networkManager;

    public Event<Void> onStarted;
    public Event<Void> onStopped;
    public Event<String> onToast;

    public BackgroundService() {
        Callback<Void> connectedCallback = new Callback<Void>() {
            @Override
            public void invoke(Void arg) {
                onStarted.invoke(null);
            }
        };
        Callback<Boolean> disconnectedCallback = new Callback<Boolean>() {
            @Override
            public void invoke(Boolean arg) {
                stop();
            }
        };
        Callback<Exception> errorCallback = new Callback<Exception>() {
            @Override
            public void invoke(Exception arg) {
                Log.e(TAG, "Error from NetworkManager", arg);
                onToast.invoke(arg.getLocalizedMessage());
            }
        };
        Callback<TorchMessageOuterClass.TorchMessage> messageCallback = new Callback<TorchMessageOuterClass.TorchMessage>() {
            @Override
            public void invoke(TorchMessageOuterClass.TorchMessage arg) {
                handleMessage(arg);
            }
        };
        featureManagers = new HashSet<>();
        networkManager = new NetworkManager();
        onStarted = new Event<>();
        onStopped = new Event<>();
        onToast = new Event<>();
        working = false;

        networkManager.onConnected.registerCallback(connectedCallback);
        networkManager.onDisconnected.registerCallback(disconnectedCallback);
        networkManager.onError.registerCallback(errorCallback);
        networkManager.onMessage.registerCallback(messageCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new BackgroundServiceBinder();
    }

    public boolean isWorking() {
        return working;
    }

    public void start(String host, Intent resultData) {
        start(host, 27014, resultData);
    }

    public void start(String host, int port, Intent resultData) {
        if(working)
            return;
        if(host == null || host.equals("") || port < 0 || port > 65535 || resultData == null) {
            Log.e(TAG, "Invalid parameters");
        }
        else {
            working = true;
            addFeatureManager(new RecordingManager(networkManager, getRecordingSettings(resultData)));
            networkManager.connect(host, port);
        }
    }

    public void stop() {
        if(!working)
            return;
        working = false;
        for(FeatureManager manager : featureManagers)
            manager.close();
        networkManager.close();
        onStopped.invoke(null);
    }

    @Override
    public void onDestroy() {
        stop();
    }

    private void addFeatureManager(final FeatureManager manager) {
        manager.onClosed.registerCallback(new Callback<Void>() {
            @Override
            public void invoke(Void arg) {
                featureManagers.remove(manager);
            }
        });
        featureManagers.add(manager);
    }

    private void handleMessage(TorchMessageOuterClass.TorchMessage message) {
    }

    private Display getDisplay() {
        WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        return windowManager.getDefaultDisplay();
    }

    private MediaProjection getMediaProjection(Intent intent) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        return mediaProjectionManager.getMediaProjection(RESULT_OK, intent);
    }

    private RecordingSettings getRecordingSettings(Intent intent) {
        return new RecordingSettings(20, getDisplay(), getMediaProjection(intent));
    }

    public class BackgroundServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }
}
