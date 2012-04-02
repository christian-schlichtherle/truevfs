/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingSeekableByteChannel;
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
// TODO: Consider renaming this to ClutchOutputArchive in TrueZIP 8.
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

    final void assertOpen() {
        if (isClosed())
            throw new IllegalStateException(new OutputClosedException());
    }

    final void checkOpen() throws IOException {
        if (isClosed())
            throw new OutputClosedException();
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
    public final OutputSocket<E> getOutputSocket(E entry) {
        return new Output(delegate.getOutputSocket(entry));
    }

    /**
     * Closes this output service.
     * Subsequent calls to this method will just forward the call to the
     * decorated output service.
     * Subsequent calls to any other method of this output service will result in
     * an {@link OutputClosedException}, even if the call to this method fails
     * with an {@link IOException}.
     * 
     * @throws IOException on any I/O failure.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        delegate.close();
    }

    private final class Output extends DecoratingOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        protected OutputSocket<? extends E> getDelegate() throws IOException {
            checkOpen();
            return delegate;
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new DisconnectingSeekableByteChannel(
                    getBoundDelegate().newSeekableByteChannel());
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new DisconnectingOutputStream(
                    getBoundDelegate().newOutputStream());
        }
    } // Output

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

    private final class DisconnectingOutputStream
    extends DecoratingOutputStream {
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            checkOpen();
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkOpen();
            delegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            checkOpen();
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingOutputStream
}
