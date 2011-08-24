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

import de.schlichtherle.truezip.io.ResourceAccountant;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.SynchronizedInputStream;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.SynchronizedReadOnlyFile;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another input shop to add accounting and multithreading
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

    private final ResourceAccountant accountant;
    private final Lock lock;

    /**
     * Constructs a concurrent input shop.
     *
     * @param  input the shop to decorate.
     * @param  lock the synchronization lock to use for accounting streams.
     *              Though not required by the use in this class, this
     *              parameter should normally be an instance of
     *              {@link ReentrantLock} because chances are that it gets
     *              locked recursively.
     * @throws NullPointerException if {@code input} is {@code null}.
     */
    public ConcurrentInputShop(InputShop<E> input, Lock lock) {
        super(input);
        this.accountant = new ResourceAccountant(lock);
        this.lock = lock;
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
    public final int waitCloseOthers(long timeout) {
        return accountant.waitStopAccounting(timeout);
    }

    /**
     * Closes and disconnects <em>all</em> entry input streams and read only
     * file created by this concurrent input shop.
     * <i>Disconnecting</i> means that any subsequent operation on the entry
     * streams will throw an {@code IOException}, with the exception of
     * their {@code close()} method.
     */
    public final <X extends Exception>
    void closeAll(ExceptionHandler<IOException, X> handler) throws X {
        accountant.closeAll(handler);
    }

    /**
     * Closes this concurrent output shop.
     *
     * @throws IOException If any open input streams or read only files are
     *         detected.
     */
    @Override
    public final void close() throws IOException {
        this.lock.lock();
        try {
            accountant.close();
            delegate.close();
        } finally {
            this.lock.unlock();
        }
    }

    /** Needs to be externally synchronized! */
    private void assertNotShopClosed() throws IOException {
        if (accountant.isClosed())
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
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                ConcurrentInputShop.this.lock.lock();
                try {
                    assertNotShopClosed();
                    return new ConcurrentReadOnlyFile(
                            new DisconnectableReadOnlyFile(
                                getBoundSocket().newReadOnlyFile()));
                } finally {
                    ConcurrentInputShop.this.lock.unlock();
                }
            }

            @Override
            public InputStream newInputStream() throws IOException {
                ConcurrentInputShop.this.lock.lock();
                try {
                    assertNotShopClosed();
                    return new ConcurrentInputStream(
                            new DisconnectableInputStream(
                                getBoundSocket().newInputStream()));
                } finally {
                    ConcurrentInputShop.this.lock.unlock();
                }
            }
        } // Input

        return new Input();
    }

    @ThreadSafe
    private final class ConcurrentReadOnlyFile
    extends SynchronizedReadOnlyFile {
        @SuppressWarnings("LeakingThisInConstructor")
        private ConcurrentReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            if (accountant.stopAccountingFor(this))
                super.close();
        }

        /**
         * The finalizer in this class forces this stream to close in order to
         * protect the decorated stream against client applications which don't
         * always close this stream.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            if (accountant.isClosed())
                return;
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    } // ConcurrentReadOnlyFile

    @ThreadSafe
    private final class ConcurrentInputStream
    extends SynchronizedInputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        ConcurrentInputStream(InputStream in) {
            super(in);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            if (accountant.stopAccountingFor(this))
                super.close();
        }

        /**
         * The finalizer in this class forces this stream to close in order to
         * protect the decorated stream against client applications which don't
         * always close this stream.
         */
        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            if (accountant.isClosed())
                return;
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    } // ConcurrentInputStream

    @NotThreadSafe
    private static final class DisconnectableReadOnlyFile
    extends DecoratingReadOnlyFile {
        private boolean closed;

        DisconnectableReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        void assertNotClosed() throws IOException {
            if (closed)
                throw new InputClosedException();
        }

        @Override
        public long length() throws IOException {
            assertNotClosed();
            return delegate.length();
        }

        @Override
        public long getFilePointer() throws IOException {
            assertNotClosed();
            return delegate.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            assertNotClosed();
            delegate.seek(pos);
        }

        @Override
        public int read() throws IOException {
            assertNotClosed();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            return delegate.read(b, off, len);
        }

        /*@Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            delegate.readFully(b, off, len);
        }*/

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            delegate.close();
        }
    } // DisconnectableReadOnlyFile

    @NotThreadSafe
    private static final class DisconnectableInputStream
    extends DecoratingInputStream {
        private boolean closed;

        DisconnectableInputStream(InputStream in) {
            super(in);
        }

        void assertNotClosed() throws IOException {
            if (closed)
                throw new InputClosedException();
        }

        @Override
        public int read() throws IOException {
            assertNotClosed();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            return delegate.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            assertNotClosed();
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            assertNotClosed();
            return delegate.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!closed)
                delegate.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            assertNotClosed();
            delegate.reset();
        }

        @Override
        public boolean markSupported() {
            return !closed && delegate.markSupported();
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            delegate.close();
        }
    } // DisconnectableInputStream
}
