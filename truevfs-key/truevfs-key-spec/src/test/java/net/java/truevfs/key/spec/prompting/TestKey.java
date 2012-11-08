/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import net.java.truevfs.key.spec.prompting.PromptingKey;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.key.spec.safe.TestKeyStrength;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class TestKey implements PromptingKey<TestKey, TestKeyStrength> {

    private static final AtomicInteger count = new AtomicInteger();
    private final int key = count.getAndIncrement();
    private boolean changeRequested;

    @Override
    public TestKey clone() {
        try {
            return (TestKey) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void reset() { changeRequested = false; }

    @Override
    public boolean isChangeRequested() { return changeRequested; }

    @Override
    public void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(@CheckForNull Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TestKey)) return false;
        final TestKey that = (TestKey) obj;
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

    @Override
    public TestKeyStrength[] getKeyStrengthValues() {
        return TestKeyStrength.values();
    }

    @Override
    public TestKeyStrength getKeyStrength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setKeyStrength(TestKeyStrength keyStrength) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
