/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.ClosedInputException;
import global.namespace.truevfs.comp.io.ClosedStreamException;
import global.namespace.truevfs.comp.io.DisconnectingInputStream;
import global.namespace.truevfs.comp.io.DisconnectingSeekableChannel;
import global.namespace.truevfs.kernel.impl.CheckedCloseable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.Optional;

/**
 * Decorates another input service in order to disconnect any resources when this input service gets closed.
 * Once {@link #close()}d, all methods of all products of this service, including all sockets, streams etc. but
 * excluding {@link #input(String)} and all {@code close()} methods of all products will throw a
 * {@link ClosedInputException} when called.
 *
 * @param <E> the type of the entries.
 * @author Christian Schlichtherle
 * @see DisconnectingOutputContainer
 */
public final class DisconnectingInputContainer<E extends Entry> extends DecoratingInputContainer<E> {

    private final CheckedCloseable cc = new CheckedCloseable(getContainer()) {

        @Override
        protected ClosedStreamException newClosedStreamException() {
            return new ClosedInputException();
        }
    };

    public DisconnectingInputContainer(InputContainer<E> input) {
        super(input);
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
    public InputSocket<E> input(String name) {
        return new InputSocket<E>() {

            final InputSocket<E> socket = getContainer().input(name);

            @Override
            public E getTarget() throws IOException {
                return cc.checked(socket::getTarget);
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return new Stream(cc.checked(() -> socket.stream(peer)));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return new Channel(cc.checked(() -> socket.channel(peer)));
            }
        };
    }

    private final class Stream extends DisconnectingInputStream {

        Stream(InputStream in) {
            super(in);
        }

        @Override
        public boolean isOpen() {
            return cc.isOpen();
        }

        @Override
        public void close() throws IOException {
            if (isOpen()) {
                in.close();
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
