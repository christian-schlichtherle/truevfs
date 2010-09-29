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

package de.schlichtherle.truezip.io.socket.input;

import de.schlichtherle.truezip.io.SynchronizedInputStream;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SynchronizedReadOnlyFile;
import de.schlichtherle.truezip.io.socket.output.ConcurrentOutputShop;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorates an {@code CommonInputShop} to add accounting and multithreading
 * synchronization for all input streams created by the target common input.
 *
 * @see     ConcurrentOutputShop
 * @param   <CE> The type of the common entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ConcurrentInputShop<CE extends CommonEntry>
extends FilterInputShop<CE, CommonInputShop<CE>> {

    private interface DoCloseable {
        void doClose() throws IOException;
    }

    private static final String CLASS_NAME
            = ConcurrentInputShop.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The pool of all open entry streams.
     * This is implemented as a map where the keys are the streams and the
     * value is the current thread.
     * The weak hash map allows the garbage collector to pick up an entry
     * stream if there are no more references to it.
     * This reduces the likeliness of an {@link CommonInputBusyException}
     * in case a sloppy client application has forgot to close a stream before
     * the common input gets closed.
     */
    private final Map<DoCloseable, Thread> streams
            = new WeakHashMap<DoCloseable, Thread>();

    private volatile boolean stopped;

    /** Constructs a new {@code ConcurrentInputShop}. */
    public ConcurrentInputShop(final CommonInputShop<CE> target) {
        super(target);
    }

    @Override
    public CommonInputSocket<CE> newInputSocket(final CE entry)
    throws IOException {
        assert !stopped;
        assert entry != null;

        class InputSocket extends FilterInputSocket<CE> {
            InputSocket() throws IOException {
                super(ConcurrentInputShop.super.newInputSocket(entry));
            }

            @Override
            public InputStream newInputStream() throws IOException {
                synchronized (ConcurrentInputShop.this) {
                    return new EntryInputStream(super.newInputStream());
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                synchronized (ConcurrentInputShop.this) {
                    return new EntryReadOnlyFile(super.newReadOnlyFile());
                }
            }
        }
        // TODO: Check: Synchronization required?
        return new InputSocket();
    }

    /**
     * Waits until all entry input streams and read only files which have been
     * opened by <em>other threads</em> get closed or a timeout occurs.
     * If the current thread is interrupted while waiting,
     * a warning message is logged using {@code java.util.logging} and
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
        assert !stopped;

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

    /** Returns the number of streams opened by the current thread. */
    private int threadStreams() {
        final Thread thisThread = Thread.currentThread();
        int n = 0;
        for (final Thread thread : streams.values())
            if (thisThread == thread)
                n++;
        return n;
    }

    /**
     * Closes and disconnects <em>all</em> entry input streams and read only
     * file created by this common input shop.
     * <i>Disconnecting</i> means that any subsequent operation on the entry
     * streams will throw an {@code IOException}, with the exception of
     * their {@code close()} method.
     */
    public synchronized <E extends Exception>
    void closeAll(final ExceptionHandler<IOException, E> handler)
    throws E {
        assert !stopped;
        try {
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
        } finally {
            stopped = true;
        }
    }

    @Override
    public void close() throws IOException {
        stopped = true;
        super.close();
    }

    /**
     * An {@link InputStream} to read the entry data from an
     * {@link CommonInputShop}.
     * This input stream provides support for finalization and throws an
     * {@link IOException} on any subsequent attempt to read data after
     * {@link #closeAll} has been called.
     */
    private final class EntryReadOnlyFile
    extends SynchronizedReadOnlyFile
    implements DoCloseable {
        private /*volatile*/ boolean closed;

        @SuppressWarnings({ "NotifyWhileNotSynced", "LeakingThisInConstructor" })
        private EntryReadOnlyFile(final ReadOnlyFile rof) {
            super(rof, ConcurrentInputShop.this);
            assert rof != null;
            streams.put(this, Thread.currentThread());
            ConcurrentInputShop.this.notify(); // there can be only one waiting thread!
        }

        private void ensureNotStopped() throws IOException {
            if (stopped)
                throw new CommonInputClosedException();
        }

        @Override
        public long length() throws IOException {
            ensureNotStopped();
            return super.length();
        }

        @Override
        public long getFilePointer() throws IOException {
            ensureNotStopped();
            return super.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            ensureNotStopped();
            super.seek(pos);
        }

        @Override
        public int read() throws IOException {
            ensureNotStopped();
            return super.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            ensureNotStopped();
            return super.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureNotStopped();
            return super.read(b, off, len);
        }

        @Override
        public void readFully(byte[] b) throws IOException {
            ensureNotStopped();
            super.readFully(b);
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            ensureNotStopped();
            super.readFully(b, off, len);
        }

        /**
         * Closes this common entry stream and releases any resources
         * associated with it.
         * This method tolerates multiple calls to it: Only the first
         * invocation closes the underlying stream.
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        public final void close() throws IOException {
            assert ConcurrentInputShop.this == lock;
            synchronized (ConcurrentInputShop.this) {
                if (closed)
                    return;
                // Order is important!
                try {
                    doClose();
                } finally {
                    streams.remove(this);
                    ConcurrentInputShop.this.notify(); // there can be only one waiting thread!
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
            assert !closed;
            /*if (closed)
                return;*/
            // Order is important!
            closed = true;
            if (!stopped)
                super.doClose();
        }

        /**
         * The finalizer in this class forces this common entry input stream
         * to close.
         * This ensures that a common input can be updated although the client
         * application may have "forgot" to close this input stream before.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
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
    } // class EntryReadOnlyFile

    /**
     * An {@link InputStream} to read the entry data from an
     * {@link CommonInputShop}.
     * This input stream provides support for finalization and throws an
     * {@link IOException} on any subsequent attempt to read data after
     * {@link #closeAll} has been called.
     */
    private final class EntryInputStream
    extends SynchronizedInputStream
    implements DoCloseable {
        private /*volatile*/ boolean closed;

        @SuppressWarnings({ "NotifyWhileNotSynced", "LeakingThisInConstructor" })
        private EntryInputStream(final InputStream in) {
            super(in, ConcurrentInputShop.this);
            assert in != null;
            streams.put(this, Thread.currentThread());
            ConcurrentInputShop.this.notify(); // there can be only one waiting thread!
        }

        private void ensureNotStopped() throws IOException {
            if (stopped)
                throw new CommonInputClosedException();
        }

        @Override
        public int read() throws IOException {
            ensureNotStopped();
            return super.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            ensureNotStopped();
            return super.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureNotStopped();
            return super.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            ensureNotStopped();
            return super.skip(n);
        }

        @Override
        public int available() throws IOException {
            ensureNotStopped();
            return super.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!stopped)
                super.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            ensureNotStopped();
            super.reset();
        }

        @Override
        public boolean markSupported() {
            return !stopped && super.markSupported();
        }

        /**
         * Closes this common entry stream and releases any resources
         * associated with it.
         * This method tolerates multiple calls to it: Only the first
         * invocation closes the underlying stream.
         *
         * @throws IOException If an I/O exception occurs.
         */
        @Override
        public final void close() throws IOException {
            assert ConcurrentInputShop.this == lock;
            synchronized (ConcurrentInputShop.this) {
                if (closed)
                    return;
                // Order is important!
                try {
                    doClose();
                } finally {
                    streams.remove(this);
                    ConcurrentInputShop.this.notify(); // there can be only one waiting thread!
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
            assert !closed;
            /*if (closed)
                return;*/
            // Order is important!
            closed = true;
            if (!stopped)
                super.doClose();
        }

        /**
         * The finalizer in this class forces this common entry input stream
         * to close.
         * This ensures that a common input can be updated although the client
         * application may have "forgot" to close this input stream before.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
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
    } // class EntryInputStream
}
