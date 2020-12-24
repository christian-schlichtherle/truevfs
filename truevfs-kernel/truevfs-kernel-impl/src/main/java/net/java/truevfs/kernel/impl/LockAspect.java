/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.locks.Lock;

/**
 * A mixin which provides some features of its reentrant {@link #lock()}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
interface LockAspect<L extends Lock> {

    /**
     * Returns the lock.
     */
    L lock();

    /** Runs the given operation while holding the lock. */
    default <T, X extends Exception> T locked(Op<T, X> op) throws X { return Locks.using(lock()).call(op); }
}
