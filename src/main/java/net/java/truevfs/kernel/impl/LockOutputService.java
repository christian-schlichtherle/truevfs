/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.cio.*;
import net.java.truecommons.io.LockOutputStream;
import net.java.truecommons.io.LockSeekableChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorates another output service to allow concurrent access which is synchronized by a
 * {@link java.util.concurrent.locks.Lock}.
 *
 * @param <E> the type of the entries in the decorated output service.
 * @author Christian Schlichtherle
 * @see LockInputService
 */
class LockOutputService<E extends Entry> extends DecoratingOutputService<E> implements LockAspect<Lock> {

    private final Lock lock = new ReentrantLock();

    LockOutputService(OutputService<E> output) {
        super(output);
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
    public OutputSocket<E> output(E entry) {
        return new AbstractOutputSocket<E>() {

            private final OutputSocket<E> socket = container.output(entry);

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
            public OutputStream stream(InputSocket<? extends Entry> peer) throws IOException {
                return new LockOutputStream(lock, locked(new Op<OutputStream, IOException>() {

                    @Override
                    public OutputStream call() throws IOException {
                        return socket.stream(peer);
                    }
                }));
            }

            @Override
            public SeekableByteChannel channel(InputSocket<? extends Entry> peer) throws IOException {
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
