/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.LockInputStream;
import global.namespace.truevfs.comp.io.LockSeekableChannel;
import global.namespace.truevfs.comp.shed.LockAspect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorates another input service to allow concurrent access which is synchronized by a {@link Lock}.
 *
 * @param <E> the type of the entries in the decorated input service.
 * @author Christian Schlichtherle
 * @see LockOutputContainer
 */
public abstract class LockInputContainer<E extends Entry>
        extends DecoratingInputContainer<E> implements LockAspect<Lock> {

    private final Lock lock = new ReentrantLock();

    protected LockInputContainer(InputContainer<E> input) {
        super(input);
    }

    @Override
    public Lock getLock() {
        return lock;
    }

    @Override
    public void close() throws IOException {
        runLocked(() -> {
            getContainer().close();
            return null;
        });
    }

    @Override
    public Collection<E> entries() throws IOException {
        return runLocked(getContainer()::entries);
    }

    @Override
    public Optional<E> entry(String name) throws IOException {
        return runLocked(() -> getContainer().entry(name));
    }

    @Override
    public InputSocket<E> input(String name) {
        return new InputSocket<E>() {

            private final InputSocket<E> socket = getContainer().input(name);

            @Override
            public E getTarget() throws IOException {
                return runLocked(socket::getTarget);
            }

            @Override
            public InputStream stream(
                    Optional<? extends OutputSocket<? extends Entry>> peer
            ) throws IOException {
                return new LockInputStream(lock, runLocked(() -> socket.stream(peer)));
            }

            @Override
            public SeekableByteChannel channel(
                    Optional<? extends OutputSocket<? extends Entry>> peer
            ) throws IOException {
                return new LockSeekableChannel(lock, runLocked(() -> socket.channel(peer)));
            }
        };
    }
}
