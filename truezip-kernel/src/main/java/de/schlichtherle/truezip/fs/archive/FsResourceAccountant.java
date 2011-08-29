/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.ThreadGroups;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * You cannot use this class outside its package.
 * <p>
 * For synchronization, each accountant uses a lock which has to be provided
 * to its {@link #FsResourceAccountant constructor}.
 * In order to start accounting for a closeable resource,
 * call {@link #startAccountingFor(Closeable)}.
 * In order to stop accounting for a closeable resource,
 * call {@link #stopAccountingFor(Closeable)}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsResourceAccountant {

    private static final String CLASS_NAME
            = FsResourceAccountant.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private final Lock lock;
    private final Condition condition;

    /**
     * The pool of all accounted closeable resources.
     * The weak hash map allows the garbage collector to pick up a closeable
     * resource if there are no more references to it.
     */
    private volatile @Nullable Map<Closeable, Reference>
            threads = new WeakHashMap<Closeable, Reference>();

    /**
     * Constructs a new resource accountant with the given lock.
     * You MUST MAKE SURE not to use two instances of this class which share
     * the same lock!
     * Otherwise {@link #waitStopAccounting} will not work as designed!
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
     * @return {@code true} if and only if the given closeable resource is not
     *         already accounted for.
     */
    boolean startAccountingFor(final Closeable resource) {
        this.lock.lock();
        try {
            if (this.threads.containsKey(resource))
                return false;
            this.threads.put(resource, new Reference(new Account(resource)));
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Stops accounting for the given closeable resource.
     * This method should be called from the implementation of
     * {@link Closeable#close()} in the given closeable resource.
     * 
     * @param  resource the closeable resource to stop accounting for.
     * @return {@code true} if and only if the given closeable resource was
     *         accounted for.
     */
    boolean stopAccountingFor(final Closeable resource) {
        this.lock.lock();
        try {
            final Reference ref = this.threads.remove(resource);
            if (null != ref) {
                this.condition.signalAll();
                return true;
            } else {
                return false;
            }
        } finally {
            this.lock.unlock();
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
     * @param  the number of milliseconds to await the closing of resources
     *         which have been accounted for in <em>other</em> threads.
     *         If this is {@code 0}, then there is no timeout for waiting.
     * @return The number of <em>all</em> accounted closeable resources.
     */
    int waitStopAccounting(final long timeout) {
        this.lock.lock();
        try {
            int size;
            final long start = System.currentTimeMillis();
            while ((size = this.threads.size()) > threadLocalResources()) {
                long toWait;
                if (timeout > 0) {
                    toWait = timeout - (System.currentTimeMillis() - start);
                    if (toWait <= 0)
                        break;
                    if (!this.condition.await(toWait, TimeUnit.MILLISECONDS))
                        return this.threads.size(); // may have changed while waiting!
                } else {
                    this.condition.await();
                }
            }
            return size;
        } catch (InterruptedException ex) {
            logger.log(Level.WARNING, "interrupted", ex);
            return this.threads.size();
        } finally {
            this.lock.unlock();
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
        for (final Reference ref : this.threads.values()) {
            final Account account = ref.get();
            if (null != account && account.owner == currentThread)
                n++;
        }
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
    <X extends Exception>
    void closeAll(final ExceptionHandler<? super IOException, X> handler)
    throws X {
        this.lock.lock();
        try {
            for (final Iterator<Closeable> i = this.threads.keySet().iterator(); i.hasNext(); ) {
                try {
                    final Closeable closeable = i.next();
                    i.remove();
                    // This may trigger another removal, but it should cause no
                    // ConcurrentModificationException because the closeable is no
                    // more present in the map.
                    closeable.close();
                } catch (IOException ex) {
                    handler.warn(ex);
                }
            }
            assert this.threads.isEmpty();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * A simple data holder for a closeable resource and the thread which
     * started accounting for it.
     */
    private static final class Account {
        final Closeable resource;
        final Thread owner = Thread.currentThread();

        Account(final Closeable resource) {
            assert null != resource;
            this.resource = resource;
        }
    } // Account

    /**
     * A reference to an {@link Account} which can notify its
     * {@link FsResourceAccountant}.
     */
    private final class Reference extends WeakReference<Account> {
        Reference(Account account) {
            super(account, Collector.queue);
        }

        /**
         * Notifies the resource accountant of this reference.
         * Mind that this method is called even if accounting for the closeable
         * resource has been properly stopped.
         */
        void notifyAccountant() {
            final Lock lock = FsResourceAccountant.this.lock;
            lock.lock();
            try {
                FsResourceAccountant.this.condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    } // Reference

    /**
     * A high priority daemon thread which runs an endless loop in order to
     * collect account references which have been picked up by the garbage
     * collector and notify their respective resource accountant.
     * You cannot use this class outside its package.
     */
    public static final class Collector extends Thread {
        static final ReferenceQueue<Account>
                queue = new ReferenceQueue<Account>();

        static {
            new Collector().start();
        }

        Collector() {
            super(ThreadGroups.getTopLevel(), Collector.class.getName());
            setPriority(MAX_PRIORITY - 2);
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ((Reference) queue.remove()).notifyAccountant();
                } catch (InterruptedException ex) {
                    logger.log(Level.WARNING, "interrupted", ex);
                }
            }
        }
    } // Collector
}
