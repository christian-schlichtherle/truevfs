/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.ThreadGroups;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
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
 * This class is only public so that you can easily look up the Javadoc for its
 * member thread class {@link Collector}.
 * You cannot instantiate this class outside its package.
 * <p>
 * For synchronization, each accountant uses a lock which has to be provided
 * to its {@link #FsResourceAccountant constructor}.
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
public final class FsResourceAccountant {

    private final Lock lock;
    private final Condition condition;

    /**
     * The pool of all accounted closeable resources.
     * The weak hash map allows the garbage collector to pick up a closeable
     * resource if there are no more references to it.
     */
    @GuardedBy("lock")
    private final Map<Closeable, Account>
            accounts = new WeakIdentityHashMap<Closeable, Account>();

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
    FsResourceAccountant(final Lock lock) {
        this.condition = (this.lock = lock).newCondition();
    }

    /**
     * Starts accounting for the given closeable resource.
     * 
     * @param resource the closeable resource to start accounting for.
     */
    void startAccountingFor(final @WillCloseWhenClosed Closeable resource) {
        lock.lock();
        try {
            if (!accounts.containsKey(resource))
                accounts.put(resource, new Account(resource));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops accounting for the given closeable resource.
     * This method should be called from the implementation of
     * {@link Closeable#close()} in the given closeable resource.
     * 
     * @param resource the closeable resource to stop accounting for.
     */
    void stopAccountingFor(final @WillNotClose Closeable resource) {
        lock.lock();
        try {
            final Account account = accounts.remove(resource);
            if (null != account) {
                account.clear();
                account.enqueue();
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits until all closeable resources which have been started accounting
     * for by <em>other</em> threads get stopped accounting for or a timeout
     * occurs or the current thread gets interrupted, whatever happens first.
     * <p>
     * Waiting for such resources can get cancelled immediately by interrupting
     * the current thread.
     * Note that the interrupt status of the current thread will be cleared
     * then.
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
                // Note that local resources may get picked up by the garbage
                // collector, so we MUST check this on each loop cycle!
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
                // Leave interrupt status cleared.
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
            if (account.owner.get() == currentThread)
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
        return accounts.size();
    }

    /**
     * For each accounted closeable resource,
     * stops accounting for it and closes it.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller also locks the lock
     * provided to the constructor - use with care!
     */
    <X extends Exception>
    void closeAllResources(final ExceptionHandler<? super IOException, X> handler)
    throws X {
        assert null != handler;

        lock.lock();
        try {
            for (final Iterator<Closeable> i = accounts.keySet().iterator(); i.hasNext(); ) {
                final Closeable resource = i.next();
                i.remove();
                try {
                    // This should trigger another attempt to remove the
                    // closeable from the map, but this should cause no
                    // ConcurrentModificationException because the closeable
                    // has already been removed.
                    resource.close();
                } catch (IOException ex) {
                    handler.warn(ex); // may throw an exception!
                }
            }
            assert accounts.isEmpty();
        } finally {
            condition.signalAll();
            lock.unlock();
        }
    }

    /**
     * A reference to a {@link Closeable} which can notify its
     * {@link FsResourceAccountant}.
     */
    private final class Account extends WeakReference<Closeable> {
        final Reference<Thread>
                owner = new WeakReference<Thread>(Thread.currentThread());
        volatile boolean enqueued;

        Account(final Closeable resource) {
            super(resource, Collector.queue);
        }

        @Override
        public boolean enqueue() {
            // Mind the desired side effect!
            return super.enqueue() && (this.enqueued = true);
        }

        boolean isEnqueuedByGC() {
            return super.isEnqueued() && !this.enqueued;
        }

        /**
         * Notifies all resource accountant waiting threads of this reference.
         * Mind that this method is called even if accounting for the closeable
         * resource has been properly stopped.
         */
        void signalAll() {
            final Lock lock = FsResourceAccountant.this.lock;
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    } // Account

    /**
     * A high priority daemon thread which runs an endless loop in order to
     * collect account references which have been picked up by the garbage
     * collector and notify their respective resource accountant.
     * You cannot use this class outside its package.
     */
    @SuppressWarnings("PublicInnerClass")
    public static final class Collector extends Thread {
        private static final ReferenceQueue<Closeable>
                queue = new ReferenceQueue<Closeable>();

        static {
            new Collector().start();
        }

        private Collector() {
            super(ThreadGroups.getServerThreadGroup(), Collector.class.getName());
            setPriority(MAX_PRIORITY - 2);
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final Account account = (Account) queue.remove();
                    if (account.isEnqueuedByGC()) {
                        //System.runFinalization();
                        account.signalAll();
                    }
                } catch (InterruptedException ignore) {
                }
            }
        }
    } // Collector
}
