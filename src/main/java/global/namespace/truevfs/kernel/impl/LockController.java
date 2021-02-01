/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.comp.io.DecoratingInputStream;
import global.namespace.truevfs.comp.io.DecoratingOutputStream;
import global.namespace.truevfs.comp.io.DecoratingSeekableChannel;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.shed.Operation;
import global.namespace.truevfs.kernel.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.Optional;

import static global.namespace.truevfs.kernel.impl.LockingStrategy.*;

/**
 * Provides read/write locking for multi-threaded access by its clients.
 * <p>
 * This controller is a barrier for {@link global.namespace.truevfs.kernel.impl.NeedsWriteLockException}s:
 * Whenever the decorated controller chain throws a {@code NeedsWriteLockException}, the read lock gets released before
 * the write lock gets acquired and the operation gets retried.
 * <p>
 * This controller is also an emitter of and a barrier for
 * {@link global.namespace.truevfs.kernel.impl.NeedsLockRetryException}s:
 * If a lock can't get immediately acquired, then a {@code NeedsLockRetryException} gets thrown.
 * This will unwind the stack of federated file systems until the {@code LockController} for the first visited file
 * system is found.
 * This controller will then pause the current thread for a small random amount of milliseconds before retrying the
 * operation.
 *
 * @author Christian Schlichtherle
 * @see LockingStrategy
 */
abstract class LockController<E extends FsArchiveEntry> implements DelegatingArchiveController<E> {

    @Override
    public Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return timedReadOrWriteLocked(() -> getController().node(options, name));
    }

    @Override
    public void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types) throws IOException {
        timedReadOrWriteLocked(() -> {
            getController().checkAccess(options, name, types);
            return null;
        });
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        timedLocked.using(getWriteLock()).call(() -> {
            getController().setReadOnly(options, name);
            return null;
        });
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Entry.Access, Long> times) throws IOException {
        return timedLocked.using(getWriteLock()).call(() -> getController().setTime(options, name, times));
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long time) throws IOException {
        return timedLocked.using(getWriteLock()).call(() -> getController().setTime(options, name, types, time));
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new InputSocket<Entry>() {

            final InputSocket<? extends Entry> socket = getController().input(options, name);

            @Override
            public Entry getTarget() throws IOException {
                return fastLocked.using(getWriteLock()).call(socket::getTarget);
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return timedLocked.using(getWriteLock()).call(() -> new LockInputStream(socket.stream(peer)));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer)
                    throws IOException {
                return timedLocked.using(getWriteLock()).call(() -> new LockSeekableChannel(socket.channel(peer)));
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<? extends Entry> template) {
        return new OutputSocket<Entry>() {

            final OutputSocket<? extends Entry> socket = getController().output(options, name, template);

            @Override
            public Entry getTarget() throws IOException {
                return fastLocked.using(getWriteLock()).call(socket::getTarget);
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return timedLocked.using(getWriteLock()).call(() -> new LockOutputStream(socket.stream(peer)));
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer)
                    throws IOException {
                return timedLocked.using(getWriteLock()).call(() -> new LockSeekableChannel(socket.channel(peer)));
            }
        };
    }

    @Override
    public void make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<? extends Entry> template) throws IOException {
        timedLocked.using(getWriteLock()).call(() -> {
            getController().make(options, name, type, template);
            return null;
        });
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        timedLocked.using(getWriteLock()).call(() -> {
            getController().unlink(options, name);
            return null;
        });
    }

    @Override
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        timedLocked.using(getWriteLock()).call(() -> {
            getController().sync(options);
            return null;
        });
    }

    private <T> T timedReadOrWriteLocked(final Operation<T, IOException> op) throws IOException {
        try {
            return timedLocked.using(getReadLock()).call(op);
        } catch (NeedsWriteLockException e) {
            if (isReadLockedByCurrentThread()) {
                throw e;
            }
            return timedLocked.using(getWriteLock()).call(op);
        }
    }

    private final class LockInputStream extends DecoratingInputStream {

        LockInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            deadLocked.using(getWriteLock()).call(() -> {
                in.close();
                return null;
            });
        }
    }

    private final class LockOutputStream extends DecoratingOutputStream {

        LockOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            deadLocked.using(getWriteLock()).call(() -> {
                out.close();
                return null;
            });
        }
    }

    private final class LockSeekableChannel extends DecoratingSeekableChannel {

        LockSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            deadLocked.using(getWriteLock()).call(() -> {
                channel.close();
                return null;
            });
        }
    }
}
