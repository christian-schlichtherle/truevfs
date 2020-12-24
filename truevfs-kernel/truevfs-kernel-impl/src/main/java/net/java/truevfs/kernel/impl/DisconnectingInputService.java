/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.ClosedInputException;
import net.java.truecommons.io.ClosedStreamException;
import net.java.truecommons.io.DisconnectingInputStream;
import net.java.truecommons.io.DisconnectingSeekableChannel;

import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;

/**
 * Decorates another input service in order to disconnect any resources when this input service gets closed.
 * Once {@link #close()}d, all methods of all products of this service, including all sockets, streams etc. but
 * excluding {@link #input(String)} and all {@code close()} methods of all products will throw a
 * {@link net.java.truecommons.io.ClosedInputException} when called.
 *
 * @param <E> the type of the entries.
 * @author Christian Schlichtherle
 * @see DisconnectingOutputService
 */
@NotThreadSafe
final class DisconnectingInputService<E extends Entry> extends DecoratingInputService<E> {

    private final CheckedCloseable cc = new CheckedCloseable(container) {

        @Override
        ClosedStreamException newClosedStreamException() {
            return new ClosedInputException();
        }
    };

    DisconnectingInputService(@WillCloseWhenClosed InputService<E> input) {
        super(input);
    }

    @DischargesObligation
    @Override
    public void close() throws IOException {
        cc.close();
    }

    boolean isOpen() {
        return cc.isOpen();
    }

    @Override
    public int size() {
        return cc.checked(new Op<Integer, RuntimeException>() {

            @Override
            public Integer call() {
                return container.size();
            }
        });
    }

    @Override
    public Iterator<E> iterator() {
        return cc.checked(new Op<Iterator<E>, RuntimeException>() {

            @Override
            public Iterator<E> call() {
                return container.iterator();
            }
        });
    }

    @Override
    public E entry(String name) {
        return cc.checked(new Op<E, RuntimeException>() {

            @Override
            public E call() {
                return container.entry(name);
            }
        });
    }

    @Override
    public InputSocket<E> input(String name) {
        return new AbstractInputSocket<E>() {

            private final InputSocket<E> socket = container.input(name);

            @Override
            public E target() throws IOException {
                return cc.checked(new Op<E, IOException>() {

                    @Override
                    public E call() throws IOException {
                        return socket.target();
                    }
                });
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> peer) throws IOException {
                return new DisconnectingInputStreamImpl(cc.checked(new Op<InputStream, IOException>() {

                    @Override
                    public InputStream call() throws IOException {
                        return socket.stream(peer);
                    }
                }));
            }

            @Override
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                return new DisconnectingSeekableChannelImpl(cc.checked(new Op<SeekableByteChannel, IOException>() {

                    @Override
                    public SeekableByteChannel call() throws IOException {
                        return socket.channel(peer);
                    }
                }));
            }
        };
    }

    private final class DisconnectingInputStreamImpl extends DisconnectingInputStream {

        DisconnectingInputStreamImpl(@WillCloseWhenClosed InputStream in) {
            super(in);
        }

        @Override
        public boolean isOpen() {
            return cc.isOpen();
        }

        @DischargesObligation
        @Override
        public void close() throws IOException {
            if (isOpen()) {
                in.close();
            }
        }
    }

    private final class DisconnectingSeekableChannelImpl extends DisconnectingSeekableChannel {

        DisconnectingSeekableChannelImpl(@WillCloseWhenClosed SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public boolean isOpen() {
            return cc.isOpen();
        }

        @DischargesObligation
        @Override
        public void close() throws IOException {
            if (isOpen()) {
                channel.close();
            }
        }
    }
}
