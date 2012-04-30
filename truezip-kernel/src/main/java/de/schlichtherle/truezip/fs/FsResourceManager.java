/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.Maps;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Accounts for {@link Closeable} resources
 * ({@link InputStream}, {@link OutputStream} etc.) which are used in multiple
 * threads.
 * <p>
 * For synchronization, each accountant uses a lock which has to be provided
 * to its {@link #FsResourceManager constructor}.
 * In order to start accounting for a closeable resource,
 * call {@link #startAccountingFor(Closeable)}.
 * In order to stop accounting for a closeable resource,
 * call {@link #stopAccountingFor(Closeable)}.
 *
 * @see    FsResourceController
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FsResourceManager {

    /**
     * The initial capacity for the hash map accounts for the number of
     * available processors, a 90% blocking factor for typical I/O and a 2/3
     * map resize threshold.
     */
    private static final int INITIAL_CAPACITY = Maps.initialCapacity(
            Runtime.getRuntime().availableProcessors() * 10);

    /**
     * The pool of all accounted closeable resources.
     * The weak hash map allows the garbage collector to pick up a closeable
     * resource if there are no more references to it.
     */
    @GuardedBy("lock")
    private static final ConcurrentMap<Closeable, Account> accounts
            = new ConcurrentHashMap<Closeable, Account>(
                INITIAL_CAPACITY, 0.75f, INITIAL_CAPACITY);

    private final Lock lock;
    private final Condition condition;

    /**
     * Constructs a new resource accountant with the given lock.
     * You MUST MAKE SURE not to use two instances of this class which share
     * the same lock!
     * Otherwise {@link #waitForeignResources} will not work as designed!
     * 
     * @param lock the lock to use for accounting resources.
     *             Though not required by the use in this class, this
     *             parameter should normally be an instance of
     *             {@link ReentrantLock} because chances are that it gets
     *             locked recursively.
     */
    FsResourceManager(final Lock lock) {
        this.condition = (this.lock = lock).newCondition();
    }

    /**
     * Starts accounting for the given closeable resource.
     * 
     * @param resource the closeable resource to start accounting for.
     */
    void startAccountingFor(final @WillCloseWhenClosed Closeable resource) {
        if (null == accounts.get(resource))
            accounts.putIfAbsent(resource, new Account());
    }

    /**
     * Stops accounting for the given closeable resource.
     * This method should be called from the implementation of
     * {@link Closeable#close()} in the given closeable resource.
     * 
     * @param resource the closeable resource to stop accounting for.
     */
    void stopAccountingFor(final @WillNotClose Closeable resource) {
        if (null != accounts.remove(resource)) {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Waits until all closeable resources which have been started accounting
     * for by <em>other</em> threads get stopped accounting for or a timeout
     * occurs or the current thread gets interrupted, whatever happens first.
     * <p>
     * Waiting for such resources can get cancelled immediately by interrupting
     * the current thread.
     * Unless the number of closeable resources which have been accounted for
     * by <em>all</em> threads is zero, this will leave the interrupt status of
     * the current thread cleared.
     * If no such foreign resources exist, then interrupting the current thread
     * does not have any effect.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller has acquired the lock
     * provided to the constructor - use with care!
     * <p>
     * Note that this method WILL NOT WORK if any two instances of this class
     * share the same lock provided to their constructor!
     *
     * @param  timeout the number of milliseconds to await the closing of
     *         resources which have been accounted for by <em>other</em>
     *         threads once the lock has been acquired.
     *         If this is non-positive, then there is no timeout for waiting.
     * @return The number of closeable resources which have been accounted for
     *         by <em>all</em> threads.
     */
    int waitForeignResources(final long timeout) {
        lock.lock();
        try {
            try {
                long toWait = TimeUnit.MILLISECONDS.toNanos(timeout);
                // Note that even local resources may get stopped accounting
                // for by a different thread, e.g. the finalizer thread, so
                // this MUST get checked on each iteration!
                while (localResources() < totalResources()) {
                    if (0 < timeout) {
                        if (0 >= toWait)
                            break;
                        toWait = condition.awaitNanos(toWait);
                    } else {
                        condition.await();
                    }
                }
            } catch (InterruptedException cancel) {
                // Fix rare racing condition between Thread.interrupt() and
                // Condition.signalAll() events.
                final int tr = totalResources();
                if (0 == tr)
                    Thread.currentThread().interrupt();
                return tr;
            }
            return totalResources();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of closeable resources which have been accounted for
     * by the <em>current</em> thread.
     * Mind that this value may reduce concurrently, even while the lock is
     * held, so it should <em>not</em> get cached!
     * <p>
     * This method <em>must not</em> get called if the {@link #lock} is not
     * acquired!
     * 
     * @return The number of closeable resources which have been accounted for
     *         by the <em>current</em> thread.
     */
    int localResources() {
        int n = 0;
        final Thread currentThread = Thread.currentThread();
        for (final Account account : accounts.values())
            if (account.getAccountant() == this
                    && account.owner == currentThread)
                n++;
        return n;
    }

    /**
     * Returns the number of closeable resources which have been accounted for
     * by <em>all</em> threads.
     * Mind that this value may reduce concurrently, even while the lock is
     * held, so it should <em>not</em> get cached!
     * <p>
     * This method <em>must not</em> get called if the {@link #lock} is not
     * acquired!
     * 
     * @return The number of closeable resources which have been accounted for
     *         by <em>all</em> threads.
     */
    int totalResources() {
        int n = 0;
        for (final Account account : accounts.values())
            if (account.getAccountant() == this)
                n++;
        return n;
    }

    /**
     * For each accounted closeable resource,
     * stops accounting for it and closes it.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller also locks the lock
     * provided to the constructor - use with care!
     */
    <X extends IOException>
    void closeAllResources(
            final ExceptionHandler<? super IOException, X> handler)
    throws IOException {
        assert null != handler;

        lock.lock();
        try {
            for (   final Iterator<Entry<Closeable, Account>>
                        i = accounts.entrySet().iterator();
                    i.hasNext(); ) {
                final Entry<Closeable, Account> entry = i.next();
                final Account account = entry.getValue();
                if (account.getAccountant() != this)
                    continue;
                final Closeable closeable = entry.getKey();
                try {
                    // This should trigger an attempt to remove the closeable
                    // from the map, but it can cause no
                    // ConcurrentModificationException because we are using a
                    // ConcurrentHashMap.
                    closeable.close(); // could throw an IOException or a RuntimeException, e.g. a NeedsLockRetryException!
                } catch (final FsControllerException ex) {
                    assert ex instanceof FsNeedsLockRetryException : ex;
                    throw ex;
                } catch (final IOException ex) {
                    handler.warn(ex); // may throw an exception!
                }
                assert !accounts.containsKey(closeable)
                        : "closeable.close() did not call stop(this) on this resource manager!";
                // This is actually redundant.
                // In either case, it must NOT get done before a successful
                // close()!
                i.remove();
            }
        } finally {
            condition.signalAll();
            lock.unlock();
        }
    }

    private final class Account {
        final Thread owner = Thread.currentThread();

        FsResourceManager getAccountant() {
            return FsResourceManager.this;
        }
    } // Account
}
