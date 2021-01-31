/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.cio.*;
import net.java.truecommons.io.ClosedOutputException;
import net.java.truecommons.io.ClosedStreamException;
import net.java.truecommons.io.DisconnectingOutputStream;
import net.java.truecommons.io.DisconnectingSeekableChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.Optional;

/**
 * Decorates another output service in order to disconnect any resources when this output service gets closed.
 * Once {@link #close()}d, all methods of all products of this service, including all sockets, streams etc. but
 * excluding {@link #output(Entry)} and all {@code close()} methods of all products will throw a
 * {@link net.java.truecommons.io.ClosedOutputException} when called.
 *
 * @param <E> the type of the entries.
 * @author Christian Schlichtherle
 * @see DisconnectingInputService
 */
final class DisconnectingOutputService<E extends Entry> extends DecoratingOutputService<E> {

    private final CheckedCloseable cc = new CheckedCloseable(container) {

        @Override
        ClosedStreamException newClosedStreamException() {
            return new ClosedOutputException();
        }
    };

    DisconnectingOutputService(OutputService<E> output) {
        super(output);
    }

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
    public OutputSocket<E> output(E entry) {
        return new AbstractOutputSocket<E>() {

            private final OutputSocket<E> socket = container.output(entry);

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
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return new DisconnectingOutputStreamImpl(cc.checked(new Op<OutputStream, IOException>() {

                    @Override
                    public OutputStream call() throws IOException {
                        return socket.stream(peer);
                    }
                }));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return new DisconnectingSeekableChannelImpl(cc.checked(new Op<SeekableByteChannel, IOException>() {

                    @Override
                    public SeekableByteChannel call() throws IOException {
                        return socket.channel(peer);
                    }
                }));
            }
        };
    }

    private final class DisconnectingOutputStreamImpl extends DisconnectingOutputStream {

        DisconnectingOutputStreamImpl(OutputStream out) {
            super(out);
        }

        @Override
        public boolean isOpen() {
            return cc.isOpen();
        }

        @Override
        public void close() throws IOException {
            if (isOpen()) {
                out.close();
            }
        }
    }

    private final class DisconnectingSeekableChannelImpl extends DisconnectingSeekableChannel {

        DisconnectingSeekableChannelImpl(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public boolean isOpen() {
            return cc.isOpen();
        }

        @Override
        public void close() throws IOException {
            if (isOpen()) {
                channel.close();
            }
        }
    }
}
