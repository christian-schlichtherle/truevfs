/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another input shop in order to disconnect any entry resources
 * when this input shop gets closed.
 *
 * @see     DisconnectingOutputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class DisconnectingInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private boolean closed;

    /**
     * Constructs a disconnecting input shop.
     *
     * @param input the shop to decorate.
     */
    public DisconnectingInputShop(InputShop<E> input) {
        super(input);
    }

    /**
     * Disconnects this shop from its decorated shop.
     * All subsequent calls will behave as if this shop had been closed,
     * although this is not happening in this method.
     * 
     * @return {@code true} if the shop has been successfully disconnected or
     *         {@code false} if it was already disconnected or closed.
     *         
     */
    public boolean disconnect() {
        final boolean closed = this.closed;
        this.closed = true;
        return !closed;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        delegate.close();
        closed = true;
    }

    private void checkOpen() throws IOException {
        if (closed)
            throw new InputClosedException();
    }

    @Override
    public InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(DisconnectingInputShop.super.getInputSocket(name));
            }

            @Override
            public E getLocalTarget() throws IOException {
                checkOpen();
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public Entry getPeerTarget() throws IOException {
                checkOpen();
                return getBoundSocket().getPeerTarget();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                checkOpen();
                return new DisconnectingReadOnlyFile(
                        getBoundSocket().newReadOnlyFile());
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                checkOpen();
                throw new UnsupportedOperationException("TODO: Implement this!");
            }

            @Override
            public InputStream newInputStream() throws IOException {
                checkOpen();
                return new DisconnectingInputStream(
                        getBoundSocket().newInputStream());
            }
        } // Input

        return new Input();
    }

    private final class DisconnectingReadOnlyFile
    extends DecoratingReadOnlyFile {
        DisconnectingReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public long length() throws IOException {
            checkOpen();
            return delegate.length();
        }

        @Override
        public long getFilePointer() throws IOException {
            checkOpen();
            return delegate.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            checkOpen();
            delegate.seek(pos);
        }

        @Override
        public int read() throws IOException {
            checkOpen();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkOpen();
            return delegate.read(b, off, len);
        }

        /*@Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            assertNotClosed();
            delegate.readFully(b, off, len);
        }*/

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingReadOnlyFile

    private final class DisconnectingInputStream
    extends DecoratingInputStream {
        DisconnectingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            checkOpen();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkOpen();
            return delegate.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            checkOpen();
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            checkOpen();
            return delegate.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!closed)
                delegate.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            checkOpen();
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
    } // DisconnectingInputStream
}
