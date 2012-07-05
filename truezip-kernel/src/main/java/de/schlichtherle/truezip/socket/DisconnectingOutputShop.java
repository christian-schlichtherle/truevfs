/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.OutputClosedException;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another output shop in order to disconnect any resources when this
 * output shop gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this shop,
 * including all sockets, streams etc. but excluding {@link #getOutputSocket}
 * and all {@link #close()} methods, will throw an
 * {@link OutputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingInputShop
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class DisconnectingOutputShop<E extends Entry>
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

    public boolean isClosed() {
        return closed;
    }

    final void assertOpen() {
        if (isClosed()) throw new IllegalStateException(new OutputClosedException());
    }

    final void checkOpen() throws IOException {
        if (isClosed()) throw new OutputClosedException();
    }

    @Override
    public int getSize() {
        assertOpen();
        return delegate.getSize();
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
    public final OutputSocket<? extends E> getOutputSocket(E entry) {
        return SOCKET_FACTORY
                .newOutputSocket(this, delegate.getOutputSocket(entry));
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

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                    DisconnectingOutputShop<E> shop,
                    OutputSocket<? extends E> output) {
                return shop.new Nio2Output(output);
            }
        },

        OIO() {
            @Override
            <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                    DisconnectingOutputShop<E> shop,
                    OutputSocket<? extends E> output) {
                return shop.new Output(output);
            }
        };

        abstract <E extends Entry> OutputSocket<? extends E> newOutputSocket(
                DisconnectingOutputShop<E> shop,
                OutputSocket <? extends E> output);
    } // SocketFactory

    private class Nio2Output extends Output {
        Nio2Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new DisconnectingSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Output

    private class Output extends DecoratingOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        protected OutputSocket<? extends E> getBoundSocket() throws IOException {
            checkOpen();
            return getDelegate().bind(this);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return new DisconnectingOutputStream(
                    getBoundSocket().newOutputStream());
        }
    } // Output

    private final class DisconnectingSeekableByteChannel
    extends de.schlichtherle.truezip.io.DisconnectingSeekableByteChannel {

        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingSeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
            super(sbc);
        }

        @Override
        public boolean isOpen() {
            return !isClosed();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (isOpen()) delegate.close();
        }
    } // DisconnectingSeekableByteChannel

    private final class DisconnectingOutputStream
    extends de.schlichtherle.truezip.io.DisconnectingOutputStream {

        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingOutputStream(@WillCloseWhenClosed OutputStream out) {
            super(out);
        }

        @Override
        public boolean isOpen() {
            return !isClosed();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (isOpen()) delegate.close();
        }
    } // DisconnectingOutputStream
}
