/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import de.truezip.kernel.util.Threads;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides thread-local lock management services.
 * 
 * @see    NeedsLockRetryException
 * @author Christian Schlichtherle
 */
@Immutable
final class LockManagement {

    static final int WAIT_TIMEOUT_MILLIS = 100;

    private static final ThreadLocal<LockUtil>
            util = new ThreadLocal<LockUtil>() {
                @Override
                public LockUtil initialValue() {
                    return new LockUtil(ThreadLocalRandom.current());
                }
            };

    /** Can't touch this - hammer time! */
    private LockManagement() { }

    /**
     * Tries to call the given consistent operation while holding the given
     * lock.
     * <p>
     * If this is the first execution of this method on the call stack of the
     * current thread, then the lock gets acquired using {@link Lock#lock()}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason and the thrown exception chain contains a
     * {@link NeedsLockRetryException}, then the lock gets temporarily
     * released and the current thread gets paused for a small random time
     * interval before this procedure starts over again.
     * Otherwise, the exception chain gets just passed on to the caller.
     * <p>
     * If this is <em>not</em> the first execution of this method on the call
     * stack of the current thread, then the lock gets acquired using
     * {@link Lock#tryLock()} instead.
     * If this fails, an {@code NeedsLockRetryException} gets created and
     * passed to the given exception handler for mapping before finally
     * throwing the resulting exception by executing
     * {@code throw handler.fail(new NeedsLockRetryException())}.
     * Once the lock has been acquired the operation gets called.
     * If this fails for some reason then the exception chain gets just passed
     * on to the caller.
     * <p>
     * This algorithm prevents dead locks effectively by temporarily unwinding
     * the stack and releasing all locks for a small random time interval.
     * Note that this requires some minimal cooperation by the operation:
     * Whenever it throws an exception, it MUST leave its resources in a
     * consistent state so that it can get retried again!
     * Mind that this is standard requirement for any {@link FsController}.
     * 
     * @param  <T> the return type of the operation.
     * @param  <X> the exception type of the operation.
     * @param  operation The atomic operation.
     * @param  lock The lock to hold while calling the operation.
     * @return The result of the operation.
     * @throws X As thrown by the operation.
     * @throws NeedsLockRetryException See above.
     */
    static <T, X extends Exception> T
    locked(final Operation<T, X> operation, final Lock lock)
    throws X {
        final LockUtil util = LockManagement.util.get();
        if (util.locking) {
            if (!lock.tryLock())
                throw NeedsLockRetryException.get();
            try {
                return operation.call();
            } finally {
                lock.unlock();
            }
        } else {
            while (true) {
                try {
                    lock.lock();
                    util.locking = true;
                    try {
                        return operation.call();
                    } finally {
                        util.locking = false;
                        lock.unlock();
                    }
                } catch (NeedsLockRetryException ex) {
                    util.pause();
                }
            }
        }
    }

    static boolean isLocking() {
        return util.get().locking;
    }

    @NotThreadSafe
    private static final class LockUtil {
        boolean locking;
        final Random rnd;

        LockUtil(final Random rnd) { this.rnd = rnd; }

        /**
         * Delays the current thread for a random time interval between one and
         * {@link #WAIT_TIMEOUT_MILLIS} milliseconds inclusively.
         * Interrupting the current thread has no effect on this method.
         */
        void pause() {
            Threads.pause(1 + rnd.nextInt(WAIT_TIMEOUT_MILLIS));
        }
    } // LockUtil
}
