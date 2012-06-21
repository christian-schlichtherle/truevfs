/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.spec.io.InputClosedException;

/**
 * Decorates another input service in order to disconnect any resources when this
 * input service gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this service,
 * including all sockets, streams etc. but excluding {@link #input}
 * and all {@link #close()} methods, will throw an
 * {@link InputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingOutputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class DisconnectingInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

    private boolean closed;

    public DisconnectingInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
    }

    public boolean isOpen() {
        return !closed;
    }

    final void assertOpen() {
        if (!isOpen()) throw new IllegalStateException(new InputClosedException());
    }

    final void checkOpen() throws InputClosedException {
        if (!isOpen()) throw new InputClosedException();
    }

    @Override
    public int size() {
        assertOpen();
        return container.size();
    }

    @Override
    public Iterator<E> iterator() {
        assertOpen();
        return container.iterator();
    }

    @Override
    public E entry(String name) {
        assertOpen();
        return container.entry(name);
    }

    @Override
    public InputSocket<E> input(String name) {
        return new Input(container.input(name));
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
    @DischargesObligation
    public void close() throws IOException {
        closed = true;
        container.close();
    }

    private final class Input extends DecoratingInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        protected InputSocket<? extends E> socket() throws IOException {
            checkOpen();
            return socket;
        }

        @Override
        public InputStream stream(OutputSocket<? extends Entry> peer)
        throws IOException {
            return new DisconnectingInputStream(socket().stream(peer));
        }

        @Override
        public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
        throws IOException {
            return new DisconnectingSeekableChannel(socket().channel(peer));
        }
    } // Input

    private final class DisconnectingInputStream
    extends net.truevfs.kernel.spec.io.DisconnectingInputStream {

        DisconnectingInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public boolean isOpen() {
            return DisconnectingInputService.this.isOpen();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (isOpen()) in.close();
        }
    } // DisconnectingInputStream

    private final class DisconnectingSeekableChannel
    extends net.truevfs.kernel.spec.io.DisconnectingSeekableChannel {

        DisconnectingSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public boolean isOpen() {
            return DisconnectingInputService.this.isOpen() && channel.isOpen();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (isOpen()) channel.close();
        }
    } // DisconnectingSeekableChannel
}
