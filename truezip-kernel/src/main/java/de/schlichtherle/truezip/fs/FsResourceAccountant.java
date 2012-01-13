/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.ThreadGroups;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.ThreadSafe;

/**
 * Accounts for {@link Closeable} resources
 * ({@link InputStream}, {@link OutputStream} etc.) which are used in multiple
 * threads.
 * This class is only publicly accessible so that you can easily look up the
 * Javadoc for its member thread class {@link Collector}.
 * You cannot instantiate this class outside its package, however.
 * <p>
 * For synchronization, each accountant uses a lock which has to be provided
 * to its {@link #FsResourceAccountant constructor}.
 * In order to start accounting for a closeable resource,
 * call {@link #startAccountingFor(Closeable)}.
 * In order to stop accounting for a closeable resource,
 * call {@link #stopAccountingFor(Closeable)}.
 *
 * @see     FsResourceController
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsResourceAccountant {

    private static final Logger logger = Logger.getLogger(
            FsResourceAccountant.class.getName(),
            FsResourceAccountant.class.getName());

    private final Lock lock;
    private final Condition condition;

    /**
     * The pool of all accounted closeable resources.
     * The weak hash map allows the garbage collector to pick up a closeable
     * resource if there are no more references to it.
     */
    private final Map<Closeable, Account> threads
            = new WeakHashMap<Closeable, Account>();

    /**
     * Constructs a new resource accountant with the given lock.
     * You MUST MAKE SURE not to use two instances of this class which share
     * the same lock!
     * Otherwise {@link #waitOtherThreads} will not work as designed!
     * 
     * @param lock the lock to use for accounting resources.
     *             Though not required by the use in this class, this
     *             parameter should normally be an instance of
     *             {@link ReentrantLock} because chances are that it gets
     *             locked recursively.
     */
    FsResourceAccountant(final Lock lock) {
        this.condition = lock.newCondition();
        this.lock = lock;
    }

    /**
     * Starts accounting for the given closeable resource.
     * 
     * @param  resource the closeable resource to start accounting for.
     */
    void startAccountingFor(final Closeable resource) {
        lock.lock();
        try {
            if (!threads.containsKey(resource))
                threads.put(resource, new Account(resource));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops accounting for the given closeable resource.
     * This method should be called from the implementation of
     * {@link Closeable#close()} in the given closeable resource.
     * 
     * @param  resource the closeable resource to stop accounting for.
     */
    void stopAccountingFor(final Closeable resource) {
        lock.lock();
        try {
            final Account ref = threads.remove(resource);
            if (null != ref) {
                assert !ref.isEnqueued();
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits until all closeable resources which have been started accounting
     * for in <em>other</em> threads get stopped accounting for or a timeout
     * occurs.
     * If the current thread is interrupted while waiting,
     * then immediately a warning message is logged using
     * {@code java.util.logging} and control is returned to the caller.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller also locks the lock
     * provided to the constructor - use with care!
     * <p>
     * Mind that this method WILL NOT WORK if any two instances of this class
     * share the same lock that has been provided to their constructor!
     *
     * @param  timeout the number of milliseconds to await the closing of
     *         resources which have been accounted for in <em>other</em>
     *         threads.
     *         If this is {@code 0}, then there is no timeout for waiting.
     * @return The number of <em>all</em> accounted closeable resources.
     */
    int waitOtherThreads(final long timeout) {
        lock.lock();
        try {
            final long start = System.currentTimeMillis();
            while (threadLocalResources() < allResources()) {
                if (0 < timeout) {
                    final long toWait = timeout - (System.currentTimeMillis() - start);
                    if (0 >= toWait)
                        break;
                    if (!condition.await(toWait, TimeUnit.MILLISECONDS))
                        return allResources();
                } else {
                    condition.await();
                }
            }
            return allResources();
        } catch (InterruptedException ex) {
            logger.log(Level.WARNING, "interrupted", ex);
            return allResources();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of closeable resources which have been accounted for
     * in the <em>current</em> thread.
     * This method must not get called if the {@link #lock} is not locked!
     */
    private int threadLocalResources() {
        int n = 0;
        final Thread currentThread = Thread.currentThread();
        for (final Account ref : threads.values())
            if (ref.owner.get() == currentThread)
                n++;
        return n;
    }

    /**
     * Returns the number of <em>all</em> accounted closeable resources.
     * Mind that this value may reduce instantly, even while the lock is held,
     * so this value should <em>not</em> get cached!
     * 
     * @return The number of <em>all</em> accounted closeable resources.
     */
    private int allResources() {
        return threads.size();
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
        lock.lock();
        try {
            for (final Iterator<Closeable> i = threads.keySet().iterator(); i.hasNext(); ) {
                final Closeable c = i.next();
                i.remove();
                try {
                    // This may trigger another removal, but it should cause no
                    // ConcurrentModificationException because the closeable is
                    // no more present in the map.
                    c.close();
                } catch (IOException ex) {
                    handler.warn(ex);
                }
            }
            assert threads.isEmpty();
        } finally {
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

        Account(Closeable resource) {
            super(resource, Collector.queue);
        }

        /**
         * Notifies the resource accountant of this reference.
         * Mind that this method is called even if accounting for the closeable
         * resource has been properly stopped.
         */
        void signalAccountant() {
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
                    ((Account) queue.remove()).signalAccountant();
                } catch (InterruptedException ex) {
                    logger.log(Level.FINE, "interrupted", ex);
                }
            }
        }
    } // Collector
}
