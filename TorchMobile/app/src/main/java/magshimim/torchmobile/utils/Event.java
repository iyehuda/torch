package magshimim.torchmobile.utils;

import java.util.HashSet;

public class Event<T> {
    private HashSet<Callback<T>> callbacks;

    public Event() {
        callbacks = new HashSet<>();
    }

    public void registerCallback(Callback<T> callback) {
        callbacks.add(callback);
    }

    public void unregisterEvent(Callback<T> callback) {
        callbacks.remove(callback);
    }

    public void invoke(T value){
        for(Callback<T> callback : callbacks)
            callback.invoke(value);
    }
}
