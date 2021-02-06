/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.util;

import bali.Lookup;

import java.util.concurrent.locks.Lock;

/**
 * A mixin which provides some features of its {@linkplain #getLock() lock}.
 *
 * @author Christian Schlichtherle
 */
public interface LockAspect<L extends Lock> {

    /**
     * Returns the lock.
     */
    @Lookup(param = "lock")
    L getLock();

    /**
     * Runs the given operation while holding the lock.
     */
    default <T, X extends Exception> T runLocked(Operation<T, X> op) throws X {
        return Locks.using(getLock()).run(op);
    }
}
