/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.util.Arrays;

/**
 * @author Christian Schlichtherle
 */
@SuppressWarnings("serial")
class TestException extends Exception {
    final int id;

    TestException(final int id, final TestException... suppressed) {
        this.id = id;
        for (TestException ex : suppressed) {
            super.addSuppressed(ex);
        }
    }

    int getPriority() {
        return 0;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public final boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof TestException)) {
            return false;
        }
        final TestException other = (TestException) that;
        return id == other.id && getPriority() == other.getPriority() && Arrays.equals(getSuppressed(), other.getSuppressed());
    }

    @Override
    public final int hashCode() {
        throw new UnsupportedOperationException();
    }    
}
