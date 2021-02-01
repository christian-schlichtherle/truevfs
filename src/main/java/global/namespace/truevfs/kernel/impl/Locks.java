/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import java.util.concurrent.locks.Lock;

/**
 * Runs an operation while a lock is held.
 *
 * @author Christian Schlichtherle
 */
final class Locks {

    /**
     * Returns a function to decorate the given operation so that it holds the given lock while running.
     */
    static Using using(Lock lock) {
        return new Using() {

            @Override
            public <T, X extends Exception> T call(final Op<T, X> op) throws X {
                lock.lock();
                try {
                    return op.call();
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    interface Using {

        <T, X extends Exception> T call(Op<T, X> op) throws X;
    }
}
