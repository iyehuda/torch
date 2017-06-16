package magshimim.torchmobile.features;

import android.support.annotation.CallSuper;

import magshimim.torchmobile.networking.NetworkManager;
import magshimim.torchmobile.networking.protos.TorchMessageOuterClass;
import magshimim.torchmobile.utils.Callback;
import magshimim.torchmobile.utils.Event;

public abstract class FeatureManager {
    private Callback<TorchMessageOuterClass.TorchMessage> messageCallback;

    protected NetworkManager networkManager;

    public Event<Void> onClosed;

    FeatureManager(NetworkManager networkManager) {
        if(networkManager == null)
            throw new IllegalArgumentException("NetworkManager is null");
        this.networkManager = networkManager;
        messageCallback = new Callback<TorchMessageOuterClass.TorchMessage>() {
            @Override
            public void invoke(TorchMessageOuterClass.TorchMessage arg) {
                if(messageFilter(arg))
                    handleMessage(arg);
            }
        };
        onClosed = new Event<>();
    }

    @CallSuper
    public void close() {
        networkManager.onMessage.unregisterEvent(messageCallback);
        onClosed.invoke(null);
    }

    @CallSuper
    public void start () {
        networkManager.onMessage.registerCallback(messageCallback);
    }

    protected abstract void handleMessage(TorchMessageOuterClass.TorchMessage message);

    protected boolean messageFilter(TorchMessageOuterClass.TorchMessage message) {
        return true;
    }
}
