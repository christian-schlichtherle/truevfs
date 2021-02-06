/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.LockOutputStream;
import global.namespace.truevfs.comp.io.LockSeekableChannel;
import global.namespace.truevfs.comp.util.LockAspect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorates another output service to allow concurrent access which is synchronized by a {@link Lock}.
 *
 * @param <E> the type of the entries in the decorated output service.
 * @author Christian Schlichtherle
 * @see LockInputContainer
 */
public abstract class LockOutputContainer<E extends Entry>
        extends DecoratingOutputContainer<E> implements LockAspect<Lock> {

    private final Lock lock = new ReentrantLock();

    protected LockOutputContainer(OutputContainer<E> output) {
        super(output);
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
    public OutputSocket<E> output(E entry) {
        return new OutputSocket<E>() {

            private final OutputSocket<E> socket = getContainer().output(entry);

            @Override
            public E getTarget() throws IOException {
                return runLocked(socket::getTarget);
            }

            @Override
            public OutputStream stream(
                    Optional<? extends InputSocket<? extends Entry>> peer
            ) throws IOException {
                return new LockOutputStream(lock, runLocked(() -> socket.stream(peer)));
            }

            @Override
            public SeekableByteChannel channel(
                    Optional<? extends InputSocket<? extends Entry>> peer
            ) throws IOException {
                return new LockSeekableChannel(lock, runLocked(() -> socket.channel(peer)));
            }
        };
    }
}
