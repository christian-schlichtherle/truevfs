/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.io.OutputClosedException;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another output service in order to disconnect any resources when this
 * output service gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this service,
 * including all sockets, streams etc. but excluding {@link #getOutputSocket}
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

    /**
     * Constructs a disconnecting output service.
     * 
     * @param output the service to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public DisconnectingOutputService(@WillCloseWhenClosed OutputService<E> output) {
        super(output);
    }

    public boolean isClosed() {
        return closed;
    }

    final void assertOpenService() {
        if (isClosed())
            throw new IllegalStateException(new OutputClosedException());
    }

    final void checkOpenService() throws IOException {
        if (isClosed())
            throw new OutputClosedException();
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
    public final OutputSocket<E> getOutputSocket(E entry) {
        return new Output(container.getOutputSocket(entry));
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
    public void close() throws IOException {
        closed = true;
        container.close();
    }

    private final class Output extends DecoratingOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        protected OutputSocket<? extends E> getSocket() throws IOException {
            checkOpenService();
            return socket;
        }

        @Override
        public OutputStream newStream() throws IOException {
            return new DisconnectingOutputStream(
                    getBoundSocket().newStream());
        }

        @Override
        public SeekableByteChannel newChannel() throws IOException {
            return new DisconnectingSeekableChannel(
                    getBoundSocket().newChannel());
        }
    } // Output

    private final class DisconnectingOutputStream
    extends DecoratingOutputStream {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            checkOpenService();
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkOpenService();
            out.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            checkOpenService();
            out.flush();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                out.close();
        }
    } // DisconnectingOutputStream

    private final class DisconnectingSeekableChannel
    extends DecoratingSeekableChannel {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingSeekableChannel(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
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
        public int write(ByteBuffer src) throws IOException {
            checkOpenService();
            return channel.write(src);
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
        public SeekableByteChannel truncate(long size) throws IOException {
            checkOpenService();
            channel.truncate(size);
            return this;
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                channel.close();
        }
    } // DisconnectingSeekableChannel
}
