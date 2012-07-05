/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Decorates another input shop in order to disconnect any resources when this
 * input shop gets closed.
 * Once {@linkplain #close() closed}, all methods of all products of this shop,
 * including all sockets, streams etc. but excluding {@link #getInputSocket}
 * and all {@link #close()} methods, will throw an
 * {@link InputClosedException} when called.
 *
 * @param  <E> the type of the entries.
 * @see    DisconnectingOutputShop
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class DisconnectingInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private boolean closed;

    /**
     * Constructs a disconnecting input shop.
     *
     * @param input the shop to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public DisconnectingInputShop(@WillCloseWhenClosed InputShop<E> input) {
        super(input);
    }

    public boolean isClosed() {
        return closed;
    }

    final void assertOpen() {
        if (isClosed()) throw new IllegalStateException(new InputClosedException());
    }

    final void checkOpen() throws IOException {
        if (isClosed()) throw new InputClosedException();
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
    public InputSocket<? extends E> getInputSocket(String name) {
        return SOCKET_FACTORY
                .newInputSocket(this, delegate.getInputSocket(name));
    }

    /**
     * Closes this input shop.
     * Subsequent calls to this method will just forward the call to the
     * decorated input shop.
     * Subsequent calls to any other method of this input shop will result in
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
            <E extends Entry> InputSocket<? extends E> newInputSocket(
                    DisconnectingInputShop<E> shop,
                    InputSocket<? extends E> input) {
                return shop.new Nio2Input(input);
            }
        },

        OIO() {
            @Override
            <E extends Entry> InputSocket<? extends E> newInputSocket(
                    DisconnectingInputShop<E> shop,
                    InputSocket<? extends E> input) {
                return shop.new Input(input);
            }
        };

        abstract <E extends Entry> InputSocket<? extends E> newInputSocket(
                DisconnectingInputShop<E> shop,
                InputSocket <? extends E> input);
    } // SocketFactory

    private class Nio2Input extends Input {
        Nio2Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return new DisconnectingSeekableByteChannel(
                    getBoundSocket().newSeekableByteChannel());
        }
    } // Nio2Input

    private class Input extends DecoratingInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        protected InputSocket<? extends E> getBoundSocket() throws IOException {
            checkOpen();
            return getDelegate().bind(this);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return new DisconnectingReadOnlyFile(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new DisconnectingInputStream(
                    getBoundSocket().newInputStream());
        }
    } // Input

    private final class DisconnectingReadOnlyFile
    extends de.schlichtherle.truezip.rof.DisconnectingReadOnlyFile {

        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingReadOnlyFile(@WillCloseWhenClosed ReadOnlyFile rof) {
            super(rof);
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
    } // DisconnectingReadOnlyFile

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

    private final class DisconnectingInputStream
    extends de.schlichtherle.truezip.io.DisconnectingInputStream {

        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        DisconnectingInputStream(@WillCloseWhenClosed InputStream in) {
            super(in);
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
    } // DisconnectingInputStream
}
