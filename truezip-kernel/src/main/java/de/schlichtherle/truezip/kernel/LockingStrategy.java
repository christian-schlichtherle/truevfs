/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Implements a locking strategy with enumerable options to control dead lock
 * prevention.
 * Note that in order to make this class work as designed, you MUST call
 * {@link #apply} for <em>each and every</em> lock which may participate in a
 * dead lock - even if you only want to call {@link Lock#lock()}!
 * Otherwise, the lock accounting in this class will not work!
 * <p>
 * This class does not use timed waiting, so there's no point in feeding it
 * with fair locks.
 * 
 * @see    NeedsLockRetryException
 * @author Christian Schlichtherle
 */
enum LockingStrategy {

    FAST_LOCK {
        /**
         * Acquires the given lock using {@link Lock#tryLock()}.
         * 
         * @param  lock the lock to acquire.
         * @throws NeedsLockRetryException if the lock cannot get immediately
         *         acquired.
         */
        @Override
        void acquire(Lock lock) {
            if (!lock.tryLock())
                throw NeedsLockRetryException.get();
        }
    },

    TIMED_LOCK {
        /**
         * Acquires the given lock using {@link Lock#tryLock(long, TimeUnit)}.
         * 
         * @param  lock the lock to acquire.
         * @throws NeedsLockRetryException if the lock cannot get acquired
         *         after {@link #ACQUIRE_TIMEOUT_MILLIS} milliseconds or if the
         *         waiting gets interrupted.
         *         The interrupt status of the current thread gets restored in
         *         the latter case.
         */
        @Override
        void acquire(Lock lock) {
            try {
                if (!lock.tryLock(ACQUIRE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                    throw NeedsLockRetryException.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt(); // restore
                throw NeedsLockRetryException.get();
            }
        }
    },

    DEAD_LOCK {
        /**
         * Acquires the given lock using {@link Lock#lock()}.
         * 
         * @param  lock the lock to acquire.
         */
        @Override
        void acquire(Lock lock) {
            lock.lock();
        }
    };

    private static final int ARBITRATE_MAX_MILLIS = 100;
    static final int ACQUIRE_TIMEOUT_MILLIS = ARBITRATE_MAX_MILLIS;

    private static final ThreadLocal<Account> accounts = new ThreadLocalAccount();

    /**
     * Holds the given lock while calling the given operation.
     * <p>
     * If this is the first execution of this method on the call stack of the
     * current thread, then the lock gets acquired using {@link Lock#lock()}.
     * Once the lock has been acquired the operation gets called.
     * If the operation fails with a {@link NeedsLockRetryException}, then
     * the lock gets temporarily released and the current thread gets paused
     * for a small random amount of milliseconds before this algorithm starts
     * over again.
     * <p>
     * If this is <em>not</em> the first execution of this method on the call
     * stack of the current thread however, then the lock gets acquired
     * according to the strategy defined by this object.
     * If acquiring the lock fails, then a {@code NeedsLockRetryException} gets
     * thrown.
     * Once the lock has been acquired the operation gets called just as if
     * this was the first execution of this method on the call stack of the
     * current thread.
     * <p>
     * If this method is called recursively on the {@link #FAST_LOCK} strategy,
     * then dead locks get effectively prevented by temporarily unwinding the
     * stack and releasing all locks for a small random amount of milliseconds.
     * However, this requires some cooperation by the caller AND the given
     * operation: Both MUST terminate their execution in a consistent state,
     * even if a {@link NeedsLockRetryException} occurs!
     * 
     * @param  <V> the return type of the operation.
     * @param  <X> the exception type of the operation.
     * @param  lock The lock to hold while calling the operation.
     * @param  operation The operation to protect by the lock.
     * @return The result of the operation.
     * @throws X As thrown by the operation.
     * @throws NeedsLockRetryException See above.
     */
    @SuppressWarnings("SleepWhileInLoop")
    <V, X extends Exception> V apply(
            final Lock lock,
            final Operation<V, X> operation)
    throws X {
        final Account account = accounts.get();
        if (0 < account.lockCount) {
            while (true) {
                acquire(lock);
                account.lockCount++;
                try {
                    return operation.call();
                } finally {
                    account.lockCount--;
                    lock.unlock();
                }
            }
        } else {
            try {
                while (true) {
                    try {
                        lock.lock();
                        account.lockCount++;
                        try {
                            return operation.call();
                        } finally {
                            account.lockCount--;
                            lock.unlock();
                        }
                    } catch (NeedsLockRetryException ex) {
                        account.arbitrate();
                    }
                }
            } finally {
                accounts.remove();
            }
        }
    }

    /**
     * Returns wether or not the current thread is holding any locks.
     * 
     * @return Wether or not the current thread is holding any locks.
     */
    static boolean isLocking() {
        return 0 < accounts.get().lockCount;
    }

    static int getLockCount() {
        return accounts.get().lockCount;
    }
    
    /**
     * Acquires the given lock.
     * 
     * @param  lock the lock to acquire.
     * @throws NeedsLockRetryException if the lock is not available.
     */
    abstract void acquire(Lock lock);

    @ThreadSafe
    private static final class ThreadLocalAccount extends ThreadLocal<Account> {
        @Override
        public Account initialValue() {
            return new Account(ThreadLocalRandom.current());
        }
    } // ThreadLocalAccount

    @NotThreadSafe
    private static final class Account {
        int lockCount;
        final Random rnd;

        Account(final Random rnd) { this.rnd = rnd; }

        /**
         * Puts the current thread to sleep for a small random amount of time
         * between one and {@link #ARBITRATE_MAX_MILLIS} milliseconds inclusively.
         * If interrupted, the interrupt status gets restored and the method
         * returns immediately.
         */
        void arbitrate() {
            try {
                Thread.sleep(1 + rnd.nextInt(ARBITRATE_MAX_MILLIS));
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt(); // restore
            }
        }
    } // Account
}
