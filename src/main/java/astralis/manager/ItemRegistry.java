package astralis.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Ordered registry with an exact-class lookup cache. */
public abstract class ItemRegistry<T> implements Iterable<T> {
    private final List<T> items = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends T>, T> itemsByClass = new ConcurrentHashMap<>();

    @Override
    public final Iterator<T> iterator() {
        return this.getItems().iterator();
    }

    public final <U extends T> Optional<U> findByClass(Class<U> itemType) {
        return Optional.ofNullable(this.getByClass(itemType));
    }

    /** Clears the class lookup cache while retaining registered items. */
    public final void clearClassCache() {
        this.itemsByClass.clear();
    }

    public final boolean isEmpty() {
        return this.items.isEmpty();
    }

    /** Adds an item, caches its exact class, and invokes the registration hook. */
    public final void register(T item) {

        Objects.requireNonNull(item, "item");
        this.items.add((T) item);

        Class itemType = item.getClass();
        this.itemsByClass.put(itemType, (T) item);
        this.onRegistered((T) item);
    }

    /** Registration hook invoked after the item is added and cached. */
    protected void onRegistered(T item) {}

    public final T findFirstOrNull(Predicate<? super T> predicate) {
        return this.findFirst(predicate).orElse(null);
    }

    /** Finds the first item satisfying the predicate. */
    public final Optional<T> findFirst(Predicate<? super T> predicate) {
        Iterator<T> itemIterator = this.items.iterator();

        while (itemIterator.hasNext()) {
            T item = itemIterator.next();
            if (predicate.test(item)) {
                return Optional.of((T) item);
            }
        }

        return Optional.empty();
    }

    @SafeVarargs
    public final void unregisterAll(T... itemsToRemove) {
        Object[] removalArray = itemsToRemove;
        int itemCount = removalArray.length;

        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            Object item = removalArray[itemIndex];
            this.unregister((T) item);
        }
    }

    /** Returns an unmodifiable live view of the ordered item list. */
    public final List<T> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public final Stream<T> stream() {
        return this.items.stream();
    }

    /** Removes predicate matches and any class-cache entries that still map to those items. */
    public final void removeMatching(Predicate<? super T> predicate) {
        ArrayList<T> matchingItems = new ArrayList<>();

        for (T item : this.items) {
            if (predicate.test(item)) {
                matchingItems.add(item);
            }
        }

        if (!matchingItems.isEmpty()) {
            this.items.removeAll(matchingItems);

            for (Object removedItem : matchingItems) {
                this.itemsByClass.remove(removedItem.getClass(), removedItem);
            }
        }
    }

    /** Looks up an exact runtime class, populating the cache on a list match. */
    public <U extends T> U getByClass(Class<U> itemType) {

        Objects.requireNonNull(itemType, "clazz");
        Object cachedItem = this.itemsByClass.get(itemType);

        if (cachedItem != null) {
            return (U) cachedItem;
        }

        for (T item : this.items) {
            if (item.getClass() == itemType) {
                this.itemsByClass.put(itemType, (T) item);
                return (U) item;
            }
        }

        return null;
    }

    /** Removes an item and removes its class cache entry only when it maps to that item. */
    public final void unregister(T item) {

        if (item != null && this.items.remove(item)) {
            this.itemsByClass.remove(item.getClass(), item);
        }
    }

    /** Returns a new list of items satisfying the predicate. */
    public final List<T> getMatching(Predicate<? super T> predicate) {
        ArrayList<T> matchingItems = new ArrayList<>();

        for (T item : this.items) {
            if (predicate.test(item)) {
                matchingItems.add(item);
            }
        }

        return matchingItems;
    }

    public final int size() {
        return this.items.size();
    }

    @SafeVarargs
    public final void registerAll(T... newItems) {
        Object[] additionArray = newItems;
        int itemCount = additionArray.length;
        int itemIndex = 0;

        while (itemIndex < itemCount) {
            Object item = additionArray[itemIndex];
            this.register((T) item);
            itemIndex++;
        }
    }
}
