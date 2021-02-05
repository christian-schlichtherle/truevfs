/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.shed;

import bali.Lookup;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A mixin which provides some features of its {@linkplain #getLock() read/write lock}.
 *
 * @author Christian Schlichtherle
 */
public interface ReadWriteLockAspect<L extends ReadWriteLock> {

    /**
     * Returns the read/write lock.
     */
    @Lookup(param = "lock")
    L getLock();

    /**
     * Returns the read lock.
     */
    default Lock getReadLock() {
        return getLock().readLock();
    }

    /**
     * Runs the given operation while holding the read lock.
     */
    default <T, X extends Exception> T runReadLocked(Operation<T, X> op) throws X {
        return Locks.using(getReadLock()).run(op);
    }

    /**
     * Returns the write lock.
     */
    default Lock getWriteLock() {
        return getLock().writeLock();
    }

    /**
     * Runs the given operation while holding the write lock.
     */
    default <T, X extends Exception> T runWriteLocked(Operation<T, X> op) throws X {
        return Locks.using(getWriteLock()).run(op);
    }
}
