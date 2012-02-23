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
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another input shop in order to disconnect any entry resources
 * when this input shop gets closed.
 *
 * @see     DisconnectingOutputShop
 * @param   <E> the type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class DisconnectingInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private boolean closed;

    /**
     * Constructs a disconnecting input shop.
     *
     * @param input the shop to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public DisconnectingInputShop(@WillCloseWhenClosed InputShop<E> input) {
        super(input);
    }

    /**
     * Closes this input shop.
     * Subsequent calls to this method will just forward the call to the
     * decorated input shop.
     * Subsequent calls to any other method of this input shop will result in
     * an {@link InputClosedException}, even if the call to this method fails
     * with an {@link IOException}.
     * 
     * @throws IOException on any I/O failure.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        delegate.close();
    }

    private void checkOpen() throws IOException {
        if (closed)
            throw new InputClosedException();
    }

    @Override
    public InputSocket<? extends E> getInputSocket(String name) {
        return SOCKET_FACTORY
                .newInputSocket(this, delegate.getInputSocket(name));
    }

    @Immutable
    private enum SocketFactory {
        OIO() {
            @Override
            <E extends Entry> InputSocket<? extends E> newInputSocket(
                    final DisconnectingInputShop<E> shop,
                    final InputSocket<? extends E> input) {
                return shop.new Input(input);
            }
        },

        NIO2() {
            @Override
            <E extends Entry> InputSocket<? extends E> newInputSocket(
                    final DisconnectingInputShop<E> shop,
                    final InputSocket<? extends E> input) {
                return shop.new Nio2Input(input);
            }
        };

        abstract <E extends Entry> InputSocket<? extends E> newInputSocket(
                final DisconnectingInputShop<E> shop,
                final InputSocket <? extends E> input);
    } // SocketFactory

    private class Nio2Input extends Input {
        Nio2Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            checkOpen();
            return new DisconnectingSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Input

    private class Input extends DecoratingInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        public E getLocalTarget() throws IOException {
            checkOpen();
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            checkOpen();
            return new DisconnectingReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            checkOpen();
            return new DisconnectingInputStream(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private final class DisconnectingReadOnlyFile
    extends DecoratingReadOnlyFile {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
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

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingReadOnlyFile

    private final class DisconnectingSeekableByteChannel
    extends DecoratingSeekableByteChannel {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            checkOpen();
            return delegate.read(dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            checkOpen();
            return delegate.write(src);
        }

        @Override
        public long position() throws IOException {
            checkOpen();
            return delegate.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            checkOpen();
            delegate.position(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            checkOpen();
            return delegate.size();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            checkOpen();
            delegate.truncate(size);
            return this;
        }

        @Override
        public boolean isOpen() {
            return !closed && delegate.isOpen();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingSeekableByteChannel

    private final class DisconnectingInputStream
    extends DecoratingInputStream {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingInputStream(@WillCloseWhenClosed InputStream in) {
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
