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

import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.SynchronizedOutputStream;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorates an {@code OutputShop} to add accounting and multithreading
 * synchronization for all output streams created by the target common
 * output.
 *
 * @see     ConcurrentInputShop
 * @param   <CE> The type of the common entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ConcurrentOutputShop<CE extends CommonEntry>
extends FilterOutputShop<CE, OutputShop<CE>> {

    private interface DoCloseable {
        void doClose() throws IOException;
    }

    private static final String CLASS_NAME
            = ConcurrentOutputShop.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The pool of all open entry streams.
     * This is implemented as a map where the keys are the streams and the
     * value is the current thread.
     * The weak hash map allows the garbage collector to pick up an entry
     * stream if there are no more references to it.
     * This reduces the likeliness of an {@link OutputBusyException}
     * in case a sloppy client application has forgot to close a stream before
     * the common output gets closed.
     */
    private final Map<DoCloseable, Thread> streams
            = new WeakHashMap<DoCloseable, Thread>();

    private volatile boolean shopClosed;

    /** Constructs a new {@code ConcurrentOutputShop}. */
    public ConcurrentOutputShop(final OutputShop<CE> output) {
        super(output);
    }

    private void ensureNotShopClosed() {
        if (shopClosed)
            throw new IllegalStateException(
                    new OutputClosedException());
    }

    @Override
    public OutputSocket<CE> getOutputSocket(final CE entry)
    throws IOException {

        class OutputSocket extends FilterOutputSocket<CE> {
            OutputSocket() throws IOException {
                super(ConcurrentOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                synchronized (ConcurrentOutputShop.this) {
                    return new EntryOutputStream(super.newOutputStream());
                }
            }
        }

        ensureNotShopClosed();
        if (null == entry)
            throw new NullPointerException();
        // TODO: Check: Synchronization required?
        return new OutputSocket();
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
    public synchronized int waitCloseOthers(final long timeout) {
        ensureNotShopClosed();
        final long start = System.currentTimeMillis();
        final int threadStreams = threadStreams();
        try {
            while (streams.size() > threadStreams) {
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
        } catch (InterruptedException ignored) {
            logger.warning("wait.interrupted");
        }
        return streams.size();
    }

    /**
     * Returns the number of streams opened by the current thread.
     */
    private int threadStreams() {
        final Thread thisThread = Thread.currentThread();
        int n = 0;
        for (final Thread thread : streams.values())
            if (thisThread == thread)
                n++;
        return n;
    }

    /**
     * Closes and disconnects <em>all</em> entry output streams created by this
     * common output shop.
     * <i>Disconnecting</i> means that any subsequent operation on the entry
     * streams will throw an {@code IOException}, with the exception of
     * their {@code close()} method.
     */
    public synchronized <E extends Exception>
    void closeAll(final ExceptionHandler<IOException, E> handler)
    throws E {
        ensureNotShopClosed();
        for (final Iterator<DoCloseable> it = streams.keySet().iterator();
        it.hasNext(); ) {
            try {
                try {
                    it.next().doClose();
                } finally {
                    it.remove();
                }
            } catch (IOException ioe) {
                handler.warn(ioe);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (shopClosed)
            return;
        shopClosed = true;
        super.close();
    }

    /**
     * An {@link OutputStream} to write the entry data to an
     * {@link OutputShop}.
     * This output stream provides support for finalization and throws an
     * {@link IOException} on any subsequent attempt to write data after
     * {@link #closeAll} has been called.
     */
    private final class EntryOutputStream
    extends SynchronizedOutputStream
    implements DoCloseable {
        private volatile boolean closed;

        @SuppressWarnings({ "NotifyWhileNotSynced", "LeakingThisInConstructor" })
        private EntryOutputStream(final OutputStream out) {
            super(out, ConcurrentOutputShop.this);
            assert out != null;
            streams.put(this, Thread.currentThread());
            ConcurrentOutputShop.this.notify(); // there can be only one waiting thread!
        }

        private void ensureNotShopClosed() throws IOException {
            if (closed)
                throw new OutputClosedException();
        }

        @Override
        public void write(int b) throws IOException {
            ensureNotShopClosed();
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensureNotShopClosed();
            super.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (!closed)
                super.flush();
        }

        /**
         * Closes this common entry stream and releases any resources
         * associated with it.
         * This method tolerates multiple calls to it: Only the first
         * invocation flushes and closes the underlying stream.
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        public final void close() throws IOException {
            assert ConcurrentOutputShop.this == lock;
            synchronized (lock) {
                // Order is important!
                if (closed)
                    return;
                try {
                    doClose();
                } finally {
                    streams.remove(this);
                    lock.notify(); // there can be only one waiting thread!
                }
            }
        }

        /**
         * Closes the underlying stream and marks this stream as being closed.
         * It is an fail to call this method on an already closed stream.
         * This method does <em>not</em> remove this stream from the pool.
         * This method is not synchronized!
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        public void doClose() throws IOException {
            // Order is important!
            assert !closed;
            /*if (closed)
                return;*/
            closed = true;
            IOException cause = null;
            try {
                try {
                    out.flush();
                } catch (IOException ex) {
                    throw cause = ex;
                }
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
        }

        /**
         * The finalizer in this class forces this common entry output stream
         * to close.
         * This ensures that a common output can be updated although the client
         * application may have "forgot" to close this output stream before.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                // Order is important!
                if (closed)
                    return;
                logger.finer("finalize.open");
                try {
                    doClose();
                } catch (IOException failure) {
                    logger.log(Level.FINE, "finalize.catch", failure);
                }
            } finally {
                super.finalize();
            }
        }
    } // class EntryOutputStream
}
