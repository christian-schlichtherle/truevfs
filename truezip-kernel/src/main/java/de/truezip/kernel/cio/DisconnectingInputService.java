/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.sbc.DecoratingSeekableByteChannel;
import de.truezip.kernel.io.InputClosedException;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
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
// TODO: Consider renaming this to ClutchInputArchive in TrueZIP 8.
@NotThreadSafe
public class DisconnectingInputService<E extends Entry>
extends DecoratingInputService<E, InputService<E>> {

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
        return container.size();
    }

    @Override
    public Iterator<E> iterator() {
        assertOpen();
        return container.iterator();
    }

    @Override
    public E getEntry(String name) {
        assertOpen();
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
     * @throws IOException on any I/O failure.
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
            checkOpen();
            return socket;
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new DisconnectingReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public SeekableByteChannel newChannel() throws IOException {
            return new DisconnectingSeekableByteChannel(
                    getBoundSocket().newChannel());
        }

        @Override
        public InputStream newStream() throws IOException {
            return new DisconnectingInputStream(
                    getBoundSocket().newStream());
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
            return rof.length();
        }

        @Override
        public long getFilePointer() throws IOException {
            checkOpen();
            return rof.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            checkOpen();
            rof.seek(pos);
        }

        @Override
        public int read() throws IOException {
            checkOpen();
            return rof.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkOpen();
            return rof.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                rof.close();
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
            return sbc.read(dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            checkOpen();
            return sbc.write(src);
        }

        @Override
        public long position() throws IOException {
            checkOpen();
            return sbc.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            checkOpen();
            sbc.position(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            checkOpen();
            return sbc.size();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            checkOpen();
            sbc.truncate(size);
            return this;
        }

        @Override
        public boolean isOpen() {
            return !closed && sbc.isOpen();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                sbc.close();
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
            return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkOpen();
            return in.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            checkOpen();
            return in.skip(n);
        }

        @Override
        public int available() throws IOException {
            checkOpen();
            return in.available();
        }

        @Override
        public void mark(int readlimit) {
            if (!closed)
                in.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            checkOpen();
            in.reset();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                in.close();
        }
    } // DisconnectingInputStream
}
