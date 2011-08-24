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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
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
 * Accounts for {@link Closeable} resources used in multiple threads while
 * holding a synchronization lock provided to the constructor.
 * <p>
 * In order to start accounting for a closeable resource,
 * call {@link #startAccountingFor(Closeable)}.
 * In order to stop accounting for a closeable resource,
 * call {@link #stopAccountingFor(Closeable)}.
 *
 * @param   <R> The type of the resources.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class ResourceAccountant implements Closeable {

    private static final String CLASS_NAME
            = ResourceAccountant.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private final Condition condition;
    private final Lock lock;

    /**
     * The pool of all accounted closeable resources.
     * The weak hash map allows the garbage collector to pick up a closeable
     * resource if there are no more references to it.
     */
    private volatile @Nullable Map<Closeable, ResourceReference>
            threads = new WeakHashMap<Closeable, ResourceReference>();

    /**
     * Constructs a new resource accountant with the given synchronization
     * lock.
     * 
     * @param lock the synchronization lock to use for accounting resources.
     *             Though not required by the use in this class, this
     *             parameter should normally be an instance of
     *             {@link ReentrantLock} because chances are that it gets
     *             locked recursively.
     */
    public ResourceAccountant(final Lock lock) {
        this.condition = lock.newCondition();
        this.lock = lock;
    }

    /**
     * Returns {@code true} if and only if this resource accountant has
     * been {@link #close() closed}.
     * 
     * @return {@code true} if and only if this resource accountant has
     *         been {@link #close() closed}.
     */
    public boolean isClosed() {
        return null == this.threads;
    }

    /**
     * Starts accounting for the given closeable resource.
     * 
     * @param  resource the closeable resource to start accounting for.
     * @return {@code true} if and only if the given closeable resource is not
     *         already accounted for.
     * @throws IllegalStateException if this resource accountant has already
     *         been closed.
     */
    public boolean startAccountingFor(final Closeable resource) {
        this.lock.lock();
        try {
            if (isClosed())
                throw new IllegalStateException("Already closed!");
            if (this.threads.containsKey(resource))
                return false;
            this.threads.put(resource, new ResourceReference(resource));
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
    public boolean stopAccountingFor(final Closeable resource) {
        this.lock.lock();
        try {
            if (isClosed())
                return false;
            final ResourceReference ref = this.threads.remove(resource);
            if (null == ref)
                return false;
            if (null != ref.get() && ref.getOwner() != Thread.currentThread())
                this.condition.signal();
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Waits until all closeable resources which have been started accounting
     * for in <em>other</em> threads get stopped accounting for or a timeout
     * occurs.
     * If the current thread is interrupted while waiting,
     * then immediately a warn message is logged using
     * {@code java.util.logging} and control is returned to the caller.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller also synchronizes on the
     * lock provided to the constructor - use with care!
     *
     * @param  the number of milliseconds to await the closing of resources
     *         which have been accounted for in <em>other</em> threads.
     *         If this is {@code 0}, then there is no timeout for waiting.
     * @return The number of <em>all</em> accounted closeable resources.
     */
    public int waitStop(final long timeout) {
        this.lock.lock();
        try {
            if (isClosed())
                return 0;
            final long start = System.currentTimeMillis();
            try {
                while (this.threads.size() > threadLocalResources()) {
                    long toWait;
                    if (timeout > 0) {
                        toWait = timeout - (System.currentTimeMillis() - start);
                        if (toWait <= 0)
                            break;
                    } else {
                        toWait = 0;
                    }
                    if (!this.condition.await(timeout, TimeUnit.MILLISECONDS))
                        break;
                }
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING, "interrupted", ex);
            }
            return this.threads.size();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Returns the number of closeable resources which have been accounted for
     * in the <em>current</em> thread.
     * This method must be externally synchronized!
     */
    private int threadLocalResources() {
        assert !isClosed();
        int n = 0;
        final Thread currentThread = Thread.currentThread();
        for (final ResourceReference ref : this.threads.values())
            if (ref.getOwner() == currentThread)
                n++;
        return n;
    }

    /**
     * For each accounted closeable resource,
     * stops accounting for it and closes it.
     * <p>
     * Upon return of this method, threads may immediately start accounting
     * for closeable resources again unless the caller also synchronizes on the
     * lock provided to the constructor - use with care!
     */
    public <X extends Exception>
    void closeAll(final ExceptionHandler<? super IOException, X> handler)
    throws X {
        this.lock.lock();
        try {
            if (isClosed())
                return;
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
     * Closes this resource accountant.
     *
     * @see    #waitStop(long)
     * @see    #closeAll
     * @throws IOException If any accounted closeable resources are detected.
     */
    @Override
    public void close() throws IOException {
        this.lock.lock();
        try {
            if (isClosed())
                return;
            final int size = this.threads.size();
            if (0 != size)
                throw new IOException("There are still " + size + " accounted closeable resources!");
            this.threads = null;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Accounts for a resource and its owner, which is the thread in which the
     * account is created.
     */
    private final class ResourceReference extends WeakReference<Closeable> {
        private final Thread owner = Thread.currentThread();

        ResourceReference(Closeable resource) {
            super(resource, ResourceCollector.queue);
        }

        Thread getOwner() {
            return owner;
        }

        void notifyAccountant() {
            final Lock lock = ResourceAccountant.this.lock;
            lock.lock();
            try {
                ResourceAccountant.this.condition.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    private static final class ResourceCollector extends Thread {
        static {
            new ResourceCollector().start();
        }

        static final ReferenceQueue<Closeable>
                queue = new ReferenceQueue<Closeable>();

        private ResourceCollector() {
            super("TrueZIP Resource Collector");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ((ResourceReference) queue.remove()).notifyAccountant();
                } catch (InterruptedException ex) {
                    logger.log(Level.WARNING, "interrupted", ex);
                }
            }
        }
    }
}
