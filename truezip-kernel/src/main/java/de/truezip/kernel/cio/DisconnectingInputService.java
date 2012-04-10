/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import de.truezip.kernel.io.InputClosedException;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
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
@NotThreadSafe
public final class DisconnectingInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    private boolean closed;

    /**
     * Constructs a disconnecting input service.
     *
     * @param input the service to decorate.
     */
    @CreatesObligation
    public DisconnectingInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
    }

    public boolean isClosed() {
        return closed;
    }

    final void assertOpenService() {
        if (isClosed())
            throw new IllegalStateException(new InputClosedException());
    }

    final void checkOpenService() throws IOException {
        if (isClosed())
            throw new InputClosedException();
    }

    @Override
    public int size() {
        assertOpenService();
        return container.size();
    }

    @Override
    public Iterator<E> iterator() {
        assertOpenService();
        return container.iterator();
    }

    @Override
    public E getEntry(String name) {
        assertOpenService();
        return container.getEntry(name);
    }

    @Override
    public InputSocket<E> getInputSocket(String name) {
        return new Input(container.getInputSocket(name));
    }

    /**
     * Closes this input service.
     * Subsequent calls to this method will just forward the call to the
     * decorated input service.
     * Subsequent calls to any other method of this input service will result in
     * an {@link InputClosedException}, even if the call to this method fails
     * with an {@link IOException}.
     * 
     * @throws IOException on any I/O error.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        container.close();
    }

    private final class Input extends DecoratingInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        protected InputSocket<? extends E> getSocket() throws IOException {
            checkOpenService();
            return socket;
        }

        @Override
        public InputStream stream() throws IOException {
            return new DisconnectingInputStream(
                    getBoundSocket().stream());
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return new DisconnectingSeekableChannel(
                    getBoundSocket().channel());
        }
    } // Input

    private final class DisconnectingInputStream
    extends DecoratingInputStream {

        @CreatesObligation
        DisconnectingInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            checkOpenService();
            return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkOpenService();
            return in.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            checkOpenService();
            return in.skip(n);
        }

        @Override
        public int available() throws IOException {
            checkOpenService();
            return in.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!closed)
                in.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            checkOpenService();
            in.reset();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                in.close();
        }
    } // DisconnectingInputStream

    private final class DisconnectingSeekableChannel
    extends DecoratingReadOnlyChannel {

        @CreatesObligation
        DisconnectingSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public boolean isOpen() {
            return !closed && channel.isOpen();
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            checkOpenService();
            return channel.read(dst);
        }

        @Override
        public long position() throws IOException {
            checkOpenService();
            return channel.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            checkOpenService();
            channel.position(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            checkOpenService();
            return channel.size();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                channel.close();
        }
    } // DisconnectingSeekableChannel
}
