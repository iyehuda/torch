package magshimim.torchmobile.utils;

public class ObjectContainer<T> {
    private T item;

    public ObjectContainer() {
        this(null);
    }

    public ObjectContainer(T item) {
        this.item = item;
    }

    public void set(T item) {
        this.item = item;
    }

    public T get() {
        return item;
    }

    public boolean hasItem() {
        return item != null;
    }
}
