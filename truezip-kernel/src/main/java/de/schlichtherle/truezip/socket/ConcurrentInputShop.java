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
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another input shop to add synchronization for all input streams
 * or read only files created by the decorated input shop in a multithreaded
 * environment.
 *
 * @see     ConcurrentOutputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class ConcurrentInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private volatile boolean closed;
    private final ResourceAccountant accountant;

    /**
     * Constructs a concurrent input shop.
     *
     * @param input the shop to decorate.
     */
    public ConcurrentInputShop(
            final InputShop<E> input,
            final ResourceAccountant accountant) {
        super(input);
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
                synchronized (ConcurrentInputShop.this) {
                    assertNotClosed();
                    return new ConcurrentReadOnlyFile(
                            new DisconnectableReadOnlyFile(
                                getBoundSocket().newReadOnlyFile()));
                }
            }

            @Override
            public InputStream newInputStream() throws IOException {
                synchronized (ConcurrentInputShop.this) {
                    assertNotClosed();
                    return new ConcurrentInputStream(
                            new DisconnectableInputStream(
                                getBoundSocket().newInputStream()));
                }
            }
        } // Input

        return new Input();
    }

    @ThreadSafe
    private final class ConcurrentReadOnlyFile
    extends SynchronizedReadOnlyFile {
        @SuppressWarnings("LeakingThisInConstructor")
        ConcurrentReadOnlyFile(DisconnectableReadOnlyFile rof) {
            super(rof, ConcurrentInputShop.this);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            accountant.stopAccountingFor(this);
            synchronized (ConcurrentInputShop.this) {
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
    } // ConcurrentReadOnlyFile

    @ThreadSafe
    private final class ConcurrentInputStream
    extends SynchronizedInputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        ConcurrentInputStream(DisconnectableInputStream in) {
            super(in, ConcurrentInputShop.this);
            accountant.startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            accountant.stopAccountingFor(this);
            synchronized (ConcurrentInputShop.this) {
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
    } // ConcurrentInputStream

    @NotThreadSafe
    private final class DisconnectableReadOnlyFile
    extends DecoratingReadOnlyFile {
        DisconnectableReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
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
            delegate.close();
        }
    } // DisconnectableReadOnlyFile

    @NotThreadSafe
    private final class DisconnectableInputStream
    extends DecoratingInputStream {
        DisconnectableInputStream(InputStream in) {
            super(in);
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
            delegate.close();
        }
    } // DisconnectableInputStream
}
