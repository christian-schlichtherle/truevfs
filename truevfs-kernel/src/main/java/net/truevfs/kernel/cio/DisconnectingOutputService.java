/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.io.DecoratingOutputStream;
import net.truevfs.kernel.io.DecoratingSeekableChannel;
import net.truevfs.kernel.io.OutputClosedException;

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
public final class DisconnectingOutputService<E extends Entry>
extends DecoratingOutputService<E, OutputService<E>> {

    private boolean closed;

    /**
     * Constructs a disconnecting output service.
     * 
     * @param output the service to decorate.
     */
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
    public E entry(String name) {
        assertOpenService();
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
            checkOpenService();
            return socket;
        }

        @Override
        public OutputStream stream() throws IOException {
            return new DisconnectingOutputStream(
                    boundSocket().stream());
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return new DisconnectingSeekableChannel(
                    boundSocket().channel());
        }
    } // Output

    private final class DisconnectingOutputStream
    extends DecoratingOutputStream {

        @CreatesObligation
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
        @DischargesObligation
        public void close() throws IOException {
            if (!closed)
                out.close();
        }
    } // DisconnectingOutputStream

    private final class DisconnectingSeekableChannel
    extends DecoratingSeekableChannel {

        @CreatesObligation
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
        @DischargesObligation
        public void close() throws IOException {
            if (!closed)
                channel.close();
        }
    } // DisconnectingSeekableChannel
}
