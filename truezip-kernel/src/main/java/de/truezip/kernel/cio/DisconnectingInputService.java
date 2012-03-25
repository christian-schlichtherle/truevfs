/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingSeekableByteChannel;
import de.truezip.kernel.io.InputClosedException;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another input service in order to disconnect any resources when this
 * input service gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this service,
 * including all sockets, streams etc. but excluding {@link #getInputSocket}
 * and all {@link #close()} methods, will throw an
 * {@link InputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingOutputService
 * @author Christian Schlichtherle
 */
// TODO: Consider renaming this to ClutchInputArchive in TrueZIP 8.
@NotThreadSafe
public class DisconnectingInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private boolean closed;

    /**
     * Constructs a disconnecting input service.
     *
     * @param input the service to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public DisconnectingInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
    }

    public boolean isClosed() {
        return closed;
    }

    final void assertOpen() {
        if (isClosed())
            throw new IllegalStateException(new InputClosedException());
    }

    final void checkOpen() throws IOException {
        if (isClosed())
            throw new InputClosedException();
    }

    @Override
    public int size() {
        assertOpen();
        return delegate.size();
    }

    @Override
    public Iterator<E> iterator() {
        assertOpen();
        return delegate.iterator();
    }

    @Override
    public E getEntry(String name) {
        assertOpen();
        return delegate.getEntry(name);
    }

    @Override
    public InputSocket<E> getInputSocket(String name) {
        return SOCKET_FACTORY
                .newInputSocket(this, delegate.getInputSocket(name));
    }

    /**
     * Closes this input service.
     * Subsequent calls to this method will just forward the call to the
     * decorated input service.
     * Subsequent calls to any other method of this input service will result in
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

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            <E extends Entry> InputSocket<E> newInputSocket(
                    DisconnectingInputService<E> service,
                    InputSocket<E> input) {
                return service.new Nio2Input(input);
            }
        },

        OIO() {
            @Override
            <E extends Entry> InputSocket<E> newInputSocket(
                    DisconnectingInputService<E> service,
                    InputSocket<E> input) {
                return service.new Input(input);
            }
        };

        abstract <E extends Entry> InputSocket<E> newInputSocket(
                DisconnectingInputService<E> service,
                InputSocket <E> input);
    } // SocketFactory

    private class Nio2Input extends Input {
        Nio2Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new DisconnectingSeekableByteChannel(
                    getBoundDelegate().newSeekableByteChannel());
        }
    } // Nio2Input

    private class Input extends DecoratingInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        protected InputSocket<? extends E> getBoundDelegate() throws IOException {
            checkOpen();
            return getDelegate().bind(this);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new DisconnectingReadOnlyFile(
                    getBoundDelegate().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new DisconnectingInputStream(
                    getBoundDelegate().newInputStream());
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
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingInputStream
}