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
final class DummyKey implements PromptingKey<DummyKey> {

    private static final AtomicInteger count = new AtomicInteger();
    private final int key = count.getAndIncrement();
    private boolean changeRequested;

    @Override
    public DummyKey clone() {
        try {
            return (DummyKey) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void reset() {
        changeRequested = false;
    }

    /*@Override
    public boolean isChangeRequested() { return changeRequested; }

    @Override
    public void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }*/

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(@CheckForNull Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DummyKey)) return false;
        final DummyKey that = (DummyKey) obj;
        return this.key == that.key
                && this.changeRequested == that.changeRequested;
    }

    @Override
    public int hashCode() {
        int c = 17;
        c = 31 * c + key;
        c = 31 * c + Boolean.valueOf(changeRequested).hashCode();
        return c;
    }
}
