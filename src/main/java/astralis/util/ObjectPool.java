package astralis.util;

import java.util.function.Supplier;

public class ObjectPool<T> {
    private final T[] items;
    private int nextIndex;
    private final Supplier<T> factory;

    public ObjectPool(int size, Supplier<T> factory) {

        this.nextIndex = 0;
        this.items = (T[]) (new Object[size]);

        for (int i = 0; i < size; i++) {
            this.items[i] = (T) factory.get();
        }

        this.factory = factory;
    }

    public void reset() {
        this.nextIndex = 0;
    }

    public T acquire() {
        if (this.nextIndex >= this.items.length) {
            int capacity = this.items.length;

            System.err.println("Overflow! Pool size " + capacity + " exceeded.");
            return this.factory.get();
        } else {
            return this.items[this.nextIndex++];
        }
    }
}
