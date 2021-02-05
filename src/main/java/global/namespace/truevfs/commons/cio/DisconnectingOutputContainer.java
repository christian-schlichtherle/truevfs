/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.cio;

import global.namespace.truevfs.commons.io.ClosedOutputException;
import global.namespace.truevfs.commons.io.ClosedStreamException;
import global.namespace.truevfs.commons.io.DisconnectingOutputStream;
import global.namespace.truevfs.commons.io.DisconnectingSeekableChannel;
import global.namespace.truevfs.kernel.impl.CheckedCloseable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.Optional;

/**
 * Decorates another output service in order to disconnect any resources when this output service gets closed.
 * Once {@link #close()}d, all methods of all products of this service, including all sockets, streams etc. but
 * excluding {@link #output(Entry)} and all {@code close()} methods of all products will throw a
 * {@link ClosedOutputException} when called.
 *
 * @param <E> the type of the entries.
 * @author Christian Schlichtherle
 * @see DisconnectingInputContainer
 */
public final class DisconnectingOutputContainer<E extends Entry> extends DecoratingOutputContainer<E> {

    private final CheckedCloseable cc = new CheckedCloseable(getContainer()) {

        @Override
        protected ClosedStreamException newClosedStreamException() {
            return new ClosedOutputException();
        }
    };

    public DisconnectingOutputContainer(OutputContainer<E> output) {
        super(output);
    }

    public boolean isOpen() {
        return cc.isOpen();
    }

    @Override
    public void close() throws IOException {
        cc.close();
    }

    @Override
    public Collection<E> entries() throws IOException {
        return cc.checked(getContainer()::entries);
    }

    @Override
    public Optional<E> entry(String name) throws IOException {
        return cc.checked(() -> getContainer().entry(name));
    }

    @Override
    public OutputSocket<E> output(E entry) {
        return new OutputSocket<E>() {

            final OutputSocket<E> socket = getContainer().output(entry);

            @Override
            public E getTarget() throws IOException {
                return cc.checked(socket::getTarget);
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return new Stream(cc.checked(() -> socket.stream(peer)));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return new Channel(cc.checked(() -> socket.channel(peer)));
            }
        };
    }

    private final class Stream extends DisconnectingOutputStream {

        Stream(OutputStream out) {
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

    private final class Channel extends DisconnectingSeekableChannel {

        Channel(SeekableByteChannel channel) {
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
