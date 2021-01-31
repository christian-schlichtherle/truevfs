/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.cio.*;
import net.java.truecommons.io.LockInputStream;
import net.java.truecommons.io.LockSeekableChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorates another input service to allow concurrent access which is synchronized by a
 * {@link java.util.concurrent.locks.Lock}.
 *
 * @param <E> the type of the entries in the decorated input service.
 * @author Christian Schlichtherle
 * @see LockOutputService
 */
class LockInputService<E extends Entry> extends DecoratingInputService<E> implements LockAspect<Lock> {

    private final Lock lock = new ReentrantLock();

    LockInputService(InputService<E> input) {
        super(input);
    }

    @Override
    public Lock lock() {
        return lock;
    }

    @Override
    public void close() throws IOException {
        locked(new Op<Object, IOException>() {

            @Override
            public Object call() throws IOException {
                container.close();
                return null;
            }
        });
    }

    @Override
    public int size() {
        return locked(new Op<Integer, RuntimeException>() {

            @Override
            public Integer call() {
                return container.size();
            }
        });
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("The returned iterator would not be thread-safe!");
    }

    @Override
    public E entry(String name) {
        return locked(new Op<E, RuntimeException>() {

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
                return locked(new Op<E, IOException>() {

                    @Override
                    public E call() throws IOException {
                        return socket.target();
                    }
                });
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return new LockInputStream(lock, locked(new Op<InputStream, IOException>() {

                    @Override
                    public InputStream call() throws IOException {
                        return socket.stream(peer);
                    }
                }));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer)
                    throws IOException {
                return new LockSeekableChannel(lock, locked(new Op<SeekableByteChannel, IOException>() {

                    @Override
                    public SeekableByteChannel call() throws IOException {
                        return socket.channel(peer);
                    }
                }));
            }
        };
    }
}
