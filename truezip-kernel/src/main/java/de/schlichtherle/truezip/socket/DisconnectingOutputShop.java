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
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.io.OutputClosedException;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another output shop in order to disconnect any entry resources
 * when this output shop gets closed.
 *
 * @see     DisconnectingInputShop
 * @param   <E> the type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class DisconnectingOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private boolean closed;

    /**
     * Constructs a disconnecting output shop.
     * 
     * @param output the shop to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public DisconnectingOutputShop(@WillCloseWhenClosed OutputShop<E> output) {
        super(output);
    }

    /**
     * Closes this output shop.
     * Subsequent calls to this method will just forward the call to the
     * decorated output shop.
     * Subsequent calls to any other method of this output shop will result in
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

    void checkOpen() throws IOException {
        if (closed)
            throw new OutputClosedException();
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(E entry) {
        return SOCKET_FACTORY
                .newOutputSocket(this, delegate.getOutputSocket(entry));
    }

    @Immutable
    private enum SocketFactory {
        OIO() {
            @Override
            <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                    final DisconnectingOutputShop<E> shop,
                    final OutputSocket<? extends E> output) {
                return shop.new Output(output);
            }
        },

        NIO2() {
            @Override
            <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                    final DisconnectingOutputShop<E> shop,
                    final OutputSocket<? extends E> output) {
                return shop.new Nio2Output(output);
            }
        };

        abstract <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                final DisconnectingOutputShop<E> shop,
                final OutputSocket <? extends E> output);
    } // SocketFactory

    private class Nio2Output extends Output {
        Nio2Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            checkOpen();
            return new DisconnectingSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Output

    private class Output extends DecoratingOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public E getLocalTarget() throws IOException {
            checkOpen();
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            checkOpen();
            return new DisconnectingOutputStream(
                    getBoundSocket().newOutputStream());
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
            if (!closed)
                delegate.flush();
        }

        @Override
        public void close() throws IOException {
            if (!closed)
                delegate.close();
        }
    } // DisconnectingOutputStream
}
