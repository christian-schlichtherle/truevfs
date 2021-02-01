/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import lombok.val;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Implements a locking strategy with enumerable options to control dead lock prevention.
 * Note that in order to make this class work as designed, you <strong>must</strong> call {@link Using#call(Op)} for
 * <em>each and every</em> {@linkplain #using(Lock) lock} which may participate in a dead lock!
 * Otherwise, the locking strategy will not work!
 *
 * @author Christian Schlichtherle
 * @see NeedsLockRetryException
 */
enum LockingStrategy {

    /**
     * Acquires the given lock using `Lock.tryLock()`.
     */
    fastLocked {
        @Override
        void acquire(final Lock lock) {
            if (!lock.tryLock()) {
                throw NeedsLockRetryException.apply();
            }
        }
    },

    /**
     * Acquires the given lock using `Lock.tryLock(long, TimeUnit)`.
     */
    timedLocked {
        @Override
        void acquire(final Lock lock) {
            try {
                if (!(lock.tryLock(acquireTimeoutMillis, TimeUnit.MILLISECONDS))) {
                    throw NeedsLockRetryException.apply();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt(); // restore
                throw NeedsLockRetryException.apply();
            }
        }
    },

    /**
     * Acquires the given lock using {@link Lock#lock()}.
     */
    deadLocked {
        @Override
        void acquire(Lock lock) {
            lock.lock();
        }
    };

    private static final int arbitrateMaxMillis = 100;
    static final int acquireTimeoutMillis = arbitrateMaxMillis;
    private static final ThreadLocal<Account> accounts =
            ThreadLocal.withInitial(() -> new Account(ThreadLocalRandom.current()));

    private static class Account {

        private final Random rnd;

        private int lockCount;

        Account(final Random rnd) {
            this.rnd = rnd;
        }

        void arbitrate() {
            try {
                Thread.sleep(1 + rnd.nextInt(arbitrateMaxMillis));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt(); // restore
            }
        }
    }

    static int lockCount() {
        return accounts.get().lockCount;
    }

    abstract void acquire(Lock lock);

    /**
     * Returns a function which holds the given lock while calling the given operation.
     * <p>
     * If this is the first execution of this method on the call stack of the current thread, then the lock gets
     * acquired using {@link Lock#lock()}.
     * Once the lock has been acquired the operation gets called.
     * If the operation fails with a {@link NeedsLockRetryException}, then the lock gets temporarily released and the
     * current thread gets paused for a small random amount of milliseconds before this algorithm starts over again.
     * <p>
     * If this is <em>not</em> the first execution of this method on the call stack of the current thread however, then
     * the lock gets acquired according to the strategy defined by this object.
     * If acquiring the lock fails, then a {@link NeedsLockRetryException} gets thrown.
     * Once the lock has been acquired the operation gets called just as if this was the first execution of this method
     * on the call stack of the current thread.
     * <p>
     * If this method is called recursively on the {@link #fastLocked} or {@link #timedLocked} strategy, then dead locks
     * get effectively prevented by temporarily unwinding the stack and releasing all locks for a small random amount of
     * milliseconds.
     * However, this requires some cooperation by the caller <strong>and</strong> the given operation: Both
     * <strong>must</strong> terminate their execution in a consistent state, because a {@link NeedsLockRetryException}
     * may occur anytime!
     *
     * @param lock The lock to hold while calling the operation.
     */
    final Using using(Lock lock) {
        return new Using() {

            @Override
            public <T, X extends Exception> T call(final Op<T, X> op) throws X {
                val account = accounts.get();
                if (0 < account.lockCount) {
                    acquire(lock);
                    account.lockCount += 1;
                    try {
                        return op.call();
                    } finally {
                        account.lockCount -= 1;
                        lock.unlock();
                    }
                } else {
                    try {
                        while (true) {
                            try {
                                lock.lock();
                                account.lockCount += 1;
                                try {
                                    return op.call();
                                } finally {
                                    account.lockCount -= 1;
                                    lock.unlock();
                                }
                            } catch (NeedsLockRetryException e) {
                                account.arbitrate();
                            }
                        }
                    } finally {
                        accounts.remove();
                    }
                }
            }
        };
    }

    @FunctionalInterface
    interface Using {

        <T, X extends Exception> T call(Op<T, X> op) throws X;
    }
}
