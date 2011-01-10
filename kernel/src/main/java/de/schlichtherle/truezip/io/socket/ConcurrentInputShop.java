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

import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.SynchronizedInputStream;
import de.schlichtherle.truezip.io.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SynchronizedReadOnlyFile;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates an {@code InputShop} to add accounting and multithreading
 * synchronization for all input streams or read only files created by the
 * decorated input shop.
 *
 * @see     ConcurrentOutputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ConcurrentInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

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
     * This reduces the likeliness of an {@link InputBusyException}
     * in case a sloppy client application has forgot to close a stream before
     * this input shop gets closed.
     */
    private final Map<Closeable, Thread> threads
            = new WeakHashMap<Closeable, Thread>();

    private volatile boolean closed;

    /**
     * Constructs a concurrent input shop.
     *
     * @param  input the shop to decorate.
     * @throws NullPointerException if {@code input} is {@code null}.
     */
    public ConcurrentInputShop(final InputShop<E> input) {
        super(input);
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
            logger.log(Level.WARNING, "wait.interrupted", ex);
        }
        return threads.size();
    }

    /** Returns the number of streams opened by the current thread. */
    private int threadStreams() {
        final Thread thisThread = Thread.currentThread();
        int n = 0;
        for (final Thread thread : threads.values())
            if (thisThread == thread)
                n++;
        return n;
    }

    /**
     * Closes and disconnects <em>all</em> entry input streams and read only
     * file created by this concurrent input shop.
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
     * @throws IllegalStateException If any open input streams or read only
     *         files are detected.
     * @see    #closeAll
     */
    @Override
    public synchronized final void close() throws IOException {
        if (!threads.isEmpty())
            throw new IllegalStateException();
        if (closed)
            return;
        closed = true;
        super.close();
    }

    /** Needs to be externally synchronized! */
    private void assertNotShopClosed() throws IOException {
        if (closed)
            throw new InputClosedException();
    }

    @Override
    public final InputSocket<? extends E> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(ConcurrentInputShop.super.getInputSocket(name));
            }

            @Override
            public InputStream newInputStream() throws IOException {
                synchronized (ConcurrentInputShop.this) {
                    assertNotShopClosed();
                    return new SynchronizedConcurrentInputStream(
                            new ConcurrentInputStream(
                                getBoundSocket().newInputStream()));
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                synchronized (ConcurrentInputShop.this) {
                    assertNotShopClosed();
                    return new SynchronizedConcurrentReadOnlyFile(
                            new ConcurrentReadOnlyFile(
                                getBoundSocket().newReadOnlyFile()));
                }
            }
        } // class Input

        return new Input();
    }

    private final class SynchronizedConcurrentInputStream
    extends SynchronizedInputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        SynchronizedConcurrentInputStream(final InputStream in) {
            super(in, ConcurrentInputShop.this);
            threads.put(in, Thread.currentThread());
        }

        @Override
        public void close() throws IOException {
            assert ConcurrentInputShop.this == lock;
            synchronized (lock) {
                if (closed)
                    return;
                try {
                    delegate.close();
                } finally {
                    threads.remove(delegate);
                    lock.notify(); // there can be only one waiting thread!
                }
            }
        }
    } // class SynchronizedConcurrentInputStream

    private final class SynchronizedConcurrentReadOnlyFile
    extends SynchronizedReadOnlyFile {
        @SuppressWarnings("LeakingThisInConstructor")
        private SynchronizedConcurrentReadOnlyFile(final ReadOnlyFile rof) {
            super(rof, ConcurrentInputShop.this);
            threads.put(rof, Thread.currentThread());
        }

        @Override
        public void close() throws IOException {
            assert ConcurrentInputShop.this == lock;
            synchronized (lock) {
                if (closed)
                    return;
                try {
                    delegate.close();
                } finally {
                    threads.remove(delegate);
                    lock.notify(); // there can be only one waiting thread!
                }
            }
        }
    } // class SynchronizedConcurrentReadOnlyFile

    private final class ConcurrentInputStream extends DecoratingInputStream {
        ConcurrentInputStream(final InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            assertNotShopClosed();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            assertNotShopClosed();
            return delegate.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            assertNotShopClosed();
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            assertNotShopClosed();
            return delegate.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!closed)
                delegate.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            assertNotShopClosed();
            delegate.reset();
        }

        @Override
        public boolean markSupported() {
            return !closed && delegate.markSupported();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }

        /**
         * The finalizer in this class forces this input stream to close.
         * This ensures that an input target can be updated although the
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
    } // class ConcurrentInputStream

    private final class ConcurrentReadOnlyFile extends DecoratingReadOnlyFile {
        ConcurrentReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public long length() throws IOException {
            assertNotShopClosed();
            return delegate.length();
        }

        @Override
        public long getFilePointer() throws IOException {
            assertNotShopClosed();
            return delegate.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            assertNotShopClosed();
            delegate.seek(pos);
        }

        @Override
        public int read() throws IOException {
            assertNotShopClosed();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            assertNotShopClosed();
            return delegate.read(b, off, len);
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            assertNotShopClosed();
            delegate.readFully(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }

        /**
         * The finalizer in this class forces this input read only file to
         * close.
         * This ensures that an input target can be updated although the
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
    } // class ConcurrentReadOnlyFile
}
