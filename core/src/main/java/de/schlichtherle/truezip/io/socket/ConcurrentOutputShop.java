/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.SynchronizedOutputStream;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorates an {@code OutputShop} to add accounting and multithreading
 * synchronization for all output streams created by the decorated output shop.
 *
 * @see     ConcurrentInputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ConcurrentOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private static final String CLASS_NAME
            = ConcurrentOutputShop.class.getName();
    private static final Logger LOGGER
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The pool of all open entry streams.
     * This is implemented as a map where the keys are the streams and the
     * value is the current thread.
     * The weak hash map allows the garbage collector to pick up an entry
     * stream if there are no more references to it.
     * This reduces the likeliness of an {@link OutputBusyException}
     * in case a sloppy client application has forgot to close a stream before
     * this output shop gets closed.
     */
    private final Map<Closeable, Thread> threads
            = new WeakHashMap<Closeable, Thread>();

    private volatile boolean closed;

    /**
     * Constructs a concurrent output shop.
     * 
     * @param  output the shop to decorate.
     * @throws NullPointerException if {@code output} is {@code null}.
     */
    public ConcurrentOutputShop(final OutputShop<E> output) {
        super(output);
    }

    /**
     * Waits until all entry output streams which have been opened by <em>other
     * threads</em> get closed or a timeout occurs.
     * If the current thread is interrupted while waiting,
     * a warn message is logged using {@code java.util.logging} and
     * this method returns.
     * <p>
     * Unless otherwise prevented, another thread could immediately open
     * another stream upon return of this method.
     * So there is actually no guarantee that really <em>all</em> streams
     * are closed upon return of this method - use carefully!
     *
     * @return The number of all open streams.
     */
    public synchronized final int waitCloseOthers(final long timeout) {
        if (closed)
            return 0;
        final long start = System.currentTimeMillis();
        final int threadStreams = threadStreams();
        try {
            while (threads.size() > threadStreams) {
                long toWait;
                if (timeout > 0) {
                    toWait = timeout - (System.currentTimeMillis() - start);
                    if (toWait <= 0)
                        break;
                } else {
                    toWait = 0;
                }
                System.gc(); // trigger garbage collection
                System.runFinalization(); // trigger finalizers - is this required at all?
                wait(toWait);
            }
        } catch (InterruptedException ex) {
            LOGGER.log(Level.WARNING, "wait.interrupted", ex);
        }
        return threads.size();
    }

    /**
     * Returns the number of streams opened by the current thread.
     */
    private int threadStreams() {
        final Thread thisThread = Thread.currentThread();
        int n = 0;
        for (final Thread thread : threads.values())
            if (thisThread == thread)
                n++;
        return n;
    }

    /**
     * Closes and disconnects <em>all</em> entry output streams created by this
     * concurrent output shop.
     * <i>Disconnecting</i> means that any subsequent operation on the entry
     * streams will throw an {@code IOException}, with the exception of
     * their {@code close()} method.
     */
    public synchronized final <E extends Exception>
    void closeAll(final ExceptionHandler<IOException, E> handler)
    throws E {
        if (closed)
            return;
        for (Iterator<Closeable> i = threads.keySet().iterator(); i.hasNext(); ) {
            try {
                Closeable closeable = i.next();
                i.remove();
                closeable.close();
            } catch (IOException ex) {
                handler.warn(ex);
            }
        }
        assert threads.isEmpty();
    }

    /**
     * Closes this concurrent output shop.
     *
     * @throws IllegalStateException If any open output streams are detected.
     * @see    #closeAll
     */
    @Override
    public synchronized final void close() throws IOException {
        if (!threads.isEmpty())
            throw new IllegalStateException();
        if (closed)
            return;
        closed = true;
        delegate.close();
    }

    /** Needs to be externally synchronized! */
    private void assertNotShopClosed() throws IOException {
        if (closed)
            throw new OutputClosedException();
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(final E entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(ConcurrentOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                synchronized (ConcurrentOutputShop.this) {
                    assertNotShopClosed();
                    return new SynchronizedConcurrentOutputStream(
                            new ConcurrentOutputStream(
                                getBoundSocket().newOutputStream()));
                }
            }
        } // class Output

        return new Output();
    }

    private final class SynchronizedConcurrentOutputStream
    extends SynchronizedOutputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        SynchronizedConcurrentOutputStream(final OutputStream out) {
            super(out, ConcurrentOutputShop.this);
            threads.put(out, Thread.currentThread());
        }

        @Override
        public void close() throws IOException {
            assert ConcurrentOutputShop.this == lock;
            synchronized (lock) {
                if (closed)
                    return;
                try {
                    try {
                        flush();
                    } finally {
                        delegate.close();
                    }
                } finally {
                    threads.remove(delegate);
                    lock.notify(); // there can be only one waiting thread!
                }
            }
        }
    } // class SynchronizedConcurrentOutputStream

    private final class ConcurrentOutputStream extends DecoratingOutputStream {
        private ConcurrentOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            assertNotShopClosed();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            assertNotShopClosed();
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (!closed)
                delegate.flush();
        }

        @Override
        public final void close() throws IOException {
            if (!closed) {
                try {
                    delegate.flush();
                } finally {
                    delegate.close();
                }
            }
        }

        /**
         * The finalizer in this class forces this output stream to close.
         * This ensures that an output container can be updated although the
         * client application may have "forgot" to close this instance before.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    } // class ConcurrentOutputStream
}
