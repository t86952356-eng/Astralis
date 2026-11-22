package astralis.internal;

import java.util.Iterator;

public interface TypedIterable<T> extends Iterable<T> {
    @Override
    Iterator<T> iterator();
}
