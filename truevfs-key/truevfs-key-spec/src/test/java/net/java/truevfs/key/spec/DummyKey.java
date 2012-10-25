/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class DummyKey implements SafeKey<DummyKey> {

    private static final AtomicInteger count = new AtomicInteger();
    private final int key = count.getAndIncrement();

    @Override
    public DummyKey clone() {
        try {
            return (DummyKey) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void reset() { }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(@CheckForNull Object that) {
        return that instanceof DummyKey && this.key == ((DummyKey) that).key;
    }

    @Override
    public int hashCode() { return key; }
}