/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.impl;

import de.truezip.kernel.key.SafeKey;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class DummyKey implements SafeKey<DummyKey> {

    private static volatile int count;

    private final int key = count++;
    boolean reset;

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
        reset = true;
    }

    @Override
    public boolean equals(@CheckForNull Object that) {
        return that instanceof DummyKey && this.key == ((DummyKey) that).key;
    }

    @Override
    public int hashCode() {
        return key;
    }
}