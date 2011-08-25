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
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.SynchronizedOutputStream;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another output shop to add synchronization for all output streams
 * created by the decorated output shop in a multithreaded environment.
 *
 * @see     ConcurrentInputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class ConcurrentOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private volatile boolean closed;
    private final ResourceAccountant accountant;

    /**
     * Constructs a concurrent output shop.
     * 
     * @param output the shop to decorate.
     */
    public ConcurrentOutputShop(
            final OutputShop<E> output,
            final ResourceAccountant accountant) {
        super(output);
        if (null == accountant)
            throw new NullPointerException();
        this.accountant = accountant;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed)
            return;
        closed = true;
        super.close();
    }

    private void assertNotClosed() throws IOException {
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
                    assertNotClosed();
                    return new ConcurrentOutputStream(
                        new DisconnectableOutputStream(
                            getBoundSocket().newOutputStream()));
                }
            }
        } // Output

        return new Output();
    }

    @ThreadSafe
    private final class ConcurrentOutputStream
    extends SynchronizedOutputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        ConcurrentOutputStream(DisconnectableOutputStream out) {
            super(out, ConcurrentOutputShop.this);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            accountant.stopAccountingFor(this);
            synchronized (ConcurrentOutputStream.this) {
                if (closed)
                    return;
                delegate.close();
            }
        }

        /**
         * The finalizer in this class forces this stream to close in order to
         * protect the decorated stream against client applications which don't
         * always close this stream.
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
    } // ConcurrentOutputStream

    @NotThreadSafe
    private final class DisconnectableOutputStream
    extends DecoratingOutputStream {
        DisconnectableOutputStream(OutputStream out) {
            super(out);
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
            delegate.close();
        }
    } // DisconnectableOutputStream
}
