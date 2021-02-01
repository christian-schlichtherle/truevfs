/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import bali.Lookup;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A mixin which provides some features of its read/write {@link #getLock()}.
 *
 * @author Christian Schlichtherle
 */
interface ReadWriteLockAspect<L extends ReadWriteLock> {

    /**
     * Returns the read/write lock.
     */
    @Lookup(param = "lock")
    L getLock();

    /**
     * Returns the read lock.
     */
    default Lock readLock() {
        return getLock().readLock();
    }

    /** Runs the given operation while holding the read lock. */
    default <T, X extends Exception> T readLocked(Op<T, X> op) throws X {
        return Locks.using(readLock()).call(op);
    }

    /**
     * Returns the write lock.
     */
    default Lock writeLock() {
        return getLock().writeLock();
    }

    /** Runs the given operation while holding the write lock. */
    default <T, X extends Exception> T writeLocked(Op<T, X> op) throws X {
        return Locks.using(writeLock()).call(op);
    }
}
