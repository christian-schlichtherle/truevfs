/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.spec.io.OutputClosedException;

/**
 * Decorates another output service in order to disconnect any resources when this
 * output service gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this service,
 * including all sockets, streams etc. but excluding {@link #output}
 * and all {@link #close()} methods, will throw an
 * {@link OutputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingInputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class DisconnectingOutputService<E extends Entry>
extends DecoratingOutputService<E, OutputService<E>> {

    private boolean closed;

    public DisconnectingOutputService(@WillCloseWhenClosed OutputService<E> output) {
        super(output);
    }

    public boolean isOpen() {
        return !closed;
    }

    final void assertOpen() {
        if (!isOpen()) throw new IllegalStateException(new OutputClosedException());
    }

    final void checkOpen() throws OutputClosedException {
        if (!isOpen()) throw new OutputClosedException();
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
    public final OutputSocket<E> output(E entry) {
        return new Output(container.output(entry));
    }

    /**
     * Closes this output service.
     * Subsequent calls to this method will just forward the call to the
     * decorated output service.
     * Subsequent calls to any other method of this output service will result in
     * an {@link OutputClosedException}, even if the call to this method fails
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

    private final class Output extends DecoratingOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        protected OutputSocket<? extends E> socket() throws IOException {
            checkOpen();
            return socket;
        }

        @Override
        public OutputStream stream(InputSocket<? extends Entry> peer)
        throws IOException {
            return new DisconnectingOutputStream(socket().stream(peer));
        }

        @Override
        public SeekableByteChannel channel(InputSocket<? extends Entry> peer)
        throws IOException {
            return new DisconnectingSeekableChannel(socket().channel(peer));
        }
    } // Output

    private final class DisconnectingOutputStream
    extends net.truevfs.kernel.spec.io.DisconnectingOutputStream {

        DisconnectingOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public boolean isOpen() {
            return DisconnectingOutputService.this.isOpen();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (isOpen()) out.close();
        }
    } // DisconnectingOutputStream

    private final class DisconnectingSeekableChannel
    extends net.truevfs.kernel.spec.io.DisconnectingSeekableChannel {

        DisconnectingSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public boolean isOpen() {
            return DisconnectingOutputService.this.isOpen();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (isOpen()) channel.close();
        }
    } // DisconnectingSeekableChannel
}
