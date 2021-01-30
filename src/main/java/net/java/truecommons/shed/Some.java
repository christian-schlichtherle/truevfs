/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

/**
 * An option which holds a nullable item.
 *
 * @see Option
 * @author Christian Schlichtherle
 */
final class Some<T> extends Option<T> {

    private final T item;

    Some(final T item) { this.item = item; }

    @Override
    public Iterator<T> iterator() {
        return Collections.singleton(item).iterator();
    }

    @Override
    public int size() { return 1; }

    @Override
    public boolean isEmpty() { return false; }

    @Override
    public T get() { return item; }

    @Override
    public T getOrElse(T alternative) { return item; }

    @Override
    public T orNull() { return item; }

    @Override
    public boolean equals(Object that) {
        return this == that ||
                that instanceof Some &&
                        Objects.equals(this.item, ((Some<?>) that).item);
    }

    @Override
    public int hashCode() { return Objects.hashCode(item); }
}
