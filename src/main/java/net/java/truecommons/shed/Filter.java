/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

/**
 * A generic filter for items of any type.
 *
 * @param  <T> The type of the items to filter.
 * @since  TrueCommons 1.0.11
 * @author Christian Schlichtherle
 */
public interface Filter<T> {

    /**
     * Returns {@code true} if and only if this filter accepts the given
     * {@code item}.
     *
     * @param  item the item to test.
     * @return Whether or not this filter accepts the given {@code item}.
     */
    boolean accept(T item);

    /** A filter which accepts any item. */
    Filter<Object> ACCEPT_ANY = new Filter<Object>() {
        @Override
        public boolean accept(Object item) { return true; }
    };

    /** A filter which accepts no items. */
    Filter<Object> ACCEPT_NONE = new Filter<Object>() {
        @Override
        public boolean accept(Object item) { return false; }
    };
}
