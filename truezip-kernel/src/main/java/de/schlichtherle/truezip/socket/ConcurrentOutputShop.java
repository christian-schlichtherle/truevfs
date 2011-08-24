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

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.SynchronizedOutputStream;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another output shop to add accounting and multithreading
 * synchronization for all output streams created by the decorated output shop.
 *
 * @see     ConcurrentInputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ConcurrentOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private final ResourceAccountant accountant;
    private final Lock lock;

    /**
     * Constructs a concurrent output shop.
     * 
     * @param  output the shop to decorate.
     * @param  lock the synchronization lock to use for accounting streams.
     *              Though not required by the use in this class, this
     *              parameter should normally be an instance of
     *              {@link ReentrantLock} because chances are that it gets
     *              locked recursively.
     * @throws NullPointerException if {@code output} is {@code null}.
     */
    public ConcurrentOutputShop(OutputShop<E> output, Lock lock) {
        super(output);
        this.accountant = new ResourceAccountant(lock);
        this.lock = lock;
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
    public final int waitCloseOthers(long timeout) {
        return accountant.waitStopAccounting(timeout);
    }

    /**
     * Closes and disconnects <em>all</em> entry output streams created by this
     * concurrent output shop.
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
     * @throws IllegalStateException If any open output streams are detected.
     * @see    #closeAll
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
                ConcurrentOutputShop.this.lock.lock();
                try {
                    assertNotShopClosed();
                    return new ConcurrentOutputStream(
                        new DisconnectableOutputStream(
                            getBoundSocket().newOutputStream()));
                } finally {
                    ConcurrentOutputShop.this.lock.unlock();
                }
            }
        } // Output

        return new Output();
    }

    @ThreadSafe
    private final class ConcurrentOutputStream
    extends SynchronizedOutputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        ConcurrentOutputStream(OutputStream out) {
            super(out);
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
    } // ConcurrentOutputStream

    @NotThreadSafe
    private static final class DisconnectableOutputStream
    extends DecoratingOutputStream {
        private boolean closed;

        private DisconnectableOutputStream(OutputStream out) {
            super(out);
        }

        void assertNotClosed() throws IOException {
            if (closed)
                throw new OutputClosedException();
        }

        @Override
        public void write(int b) throws IOException {
            assertNotClosed();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (!closed)
                delegate.flush();
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            delegate.close();
        }
    } // DisconnectableOutputStream
}
