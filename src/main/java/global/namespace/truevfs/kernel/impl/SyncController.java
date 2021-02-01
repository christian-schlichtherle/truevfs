/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.io.DecoratingInputStream;
import global.namespace.truevfs.comp.io.DecoratingOutputStream;
import global.namespace.truevfs.comp.io.DecoratingSeekableChannel;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.*;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.Optional;

import static global.namespace.truevfs.kernel.api.FsSyncOptions.RESET;
import static global.namespace.truevfs.kernel.api.FsSyncOptions.SYNC;

/**
 * Performs a `sync` operation if required.
 * <p>
 * This controller is a barrier for {@link global.namespace.truevfs.kernel.impl.NeedsSyncException}s:
 * Whenever the decorated controller chain throws a {@code NeedsSyncException}, the file system gets {@code sync}ed
 * before the operation gets retried.
 *
 * @author Christian Schlichtherle
 * @see NeedsSyncException
 */
abstract class SyncController<E extends FsArchiveEntry> implements DelegatingArchiveController<E> {

    private static final BitField<FsSyncOption> NOT_WAIT_CLOSE_IO = BitField.of(FsSyncOption.WAIT_CLOSE_IO).not();

    static BitField<FsSyncOption> modify(BitField<FsSyncOption> options) {
        return 1 >= LockingStrategy.lockCount() ? options : options.and(NOT_WAIT_CLOSE_IO);
    }

    @Override
    public Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return apply(new Op<Optional<? extends FsNode>, IOException>() {

            @Override
            public Optional<? extends FsNode> call() throws IOException {
                return getController().node(options, name);
            }
        });
    }

    @Override
    public void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types) throws IOException {
        apply(new Op<Void, IOException>() {

            @Override
            public Void call() throws IOException {
                getController().checkAccess(options, name, types);
                return null;
            }
        });
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        apply(new Op<Void, IOException>() {

            @Override
            public Void call() throws IOException {
                getController().setReadOnly(options, name);
                return null;
            }
        });
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Entry.Access, Long> times) throws IOException {
        return apply(new Op<Boolean, IOException>() {

            @Override
            public Boolean call() throws IOException {
                return getController().setTime(options, name, times);
            }
        });
    }

    @Override
    public boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long time) throws IOException {
        return apply(new Op<Boolean, IOException>() {

            @Override
            public Boolean call() throws IOException {
                return getController().setTime(options, name, types, time);
            }
        });
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        return new AbstractInputSocket<Entry>() {

            final InputSocket<? extends Entry> socket = getController().input(options, name);

            @Override
            public Entry target() throws IOException {
                return apply(new Op<Entry, IOException>() {

                    @Override
                    public Entry call() throws IOException {
                        return socket.target();
                    }
                });
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return apply(new Op<InputStream, IOException>() {

                    @Override
                    public InputStream call() throws IOException {
                        return new SyncInputStream(socket.stream(peer));
                    }
                });
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return apply(new Op<SeekableByteChannel, IOException>() {

                    @Override
                    public SeekableByteChannel call() throws IOException {
                        return new SyncSeekableChannel(socket.channel(peer));
                    }
                });
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<? extends Entry> template) {
        return new AbstractOutputSocket<Entry>() {

            final OutputSocket<? extends Entry> socket = getController().output(options, name, template);

            @Override
            public Entry target() throws IOException {
                return apply(new Op<Entry, IOException>() {

                    @Override
                    public Entry call() throws IOException {
                        return socket.target();
                    }
                });
            }

            @Override
            public OutputStream stream(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                return apply(new Op<OutputStream, IOException>() {

                    @Override
                    public OutputStream call() throws IOException {
                        return new SyncOutputStream(socket.stream(peer));
                    }
                });
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer)
                    throws IOException {
                return apply(new Op<SeekableByteChannel, IOException>() {

                    @Override
                    public SeekableByteChannel call() throws IOException {
                        return new SyncSeekableChannel(socket.channel(peer));
                    }
                });
            }
        };
    }

    @Override
    public void make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<? extends Entry> template) throws IOException {
        apply(new Op<Void, IOException>() {

            @Override
            public Void call() throws IOException {
                getController().make(options, name, type, template);
                return null;
            }
        });
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        apply(new Op<Void, IOException>() {

            @Override
            public Void call() throws IOException {
                // HC SVNT DRACONES!
                getController().unlink(options, name);
                // Eventually make the file system controller chain eligible for GC.
                if (name.isRoot()) {
                    getController().sync(RESET);
                }
                return null;
            }
        });
    }

    /**
     * Syncs the super class controller if needed and applies the given file system operation.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions apply.
     *                                This implies that the respective parent file system has been synchronized with
     *                                constraints, e.g. if an unclosed archive entry stream gets forcibly closed.
     * @throws FsSyncException        if any error conditions apply.
     * @throws IOException            at the discretion of {@code operation}.
     */
    private <T> T apply(final Op<T, IOException> op) throws IOException {
        while (true) {
            try {
                return op.call();
            } catch (final NeedsSyncException e1) {
                checkWriteLockedByCurrentThread();
                try {
                    doSync(SYNC);
                } catch (final FsSyncException e2) {
                    e2.addSuppressed(e1);
                    throw e2;
                }
            }
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        assert writeLockedByCurrentThread();
        assert !readLockedByCurrentThread();

        doSync(options);
    }

    /**
     * Performs a sync on the super class controller whereby the sync options are modified so that no dead lock can
     * appear due to waiting for I/O resources in a recursive file system operation.
     *
     * @param options the sync options
     * @throws FsSyncWarningException  if <em>only</em> warning conditions apply.
     *                                 This implies that the respective parent file system has been synchronized with
     *                                 constraints, e.g. if an unclosed archive entry stream gets forcibly closed.
     * @throws FsSyncException         if any error conditions apply.
     * @throws NeedsLockRetryException if a lock retry is needed.
     */
    private void doSync(final BitField<FsSyncOption> options) throws FsSyncException {
        // HC SVNT DRACONES!
        val modified = modify(options);
        boolean done = false;
        do {
            try {
                getController().sync(modified);
                done = true;
            } catch (final FsSyncException e) {
                if (e.getCause() instanceof FsOpenResourceException && modified != options) {
                    assert !(e instanceof FsSyncWarningException);
                    // Swallow exception:
                    throw NeedsLockRetryException.apply();
                } else {
                    throw e;
                }
            } catch (NeedsSyncException yeahIKnow_IWasActuallyDoingThat) {
                // This exception was thrown by the resource controller in
                // order to indicate that the state of the virtual file
                // system may have completely changed as a side effect of
                // temporarily releasing its write lock.
                // The sync operation needs to get repeated.
            }
        } while (!done);
    }

    private final class SyncInputStream extends DecoratingInputStream {

        SyncInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            apply(new Op<Void, IOException>() {

                @Override
                public Void call() throws IOException {
                    in.close();
                    return null;
                }
            });
        }
    }

    private final class SyncOutputStream extends DecoratingOutputStream {

        SyncOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            apply(new Op<Void, IOException>() {

                @Override
                public Void call() throws IOException {
                    out.close();
                    return null;
                }
            });
        }
    }

    private final class SyncSeekableChannel extends DecoratingSeekableChannel {

        SyncSeekableChannel(SeekableByteChannel channel) {
            super(channel);
        }

        @Override
        public void close() throws IOException {
            apply(new Op<Void, IOException>() {

                @Override
                public Void call() throws IOException {
                    channel.close();
                    return null;
                }
            });
        }
    }

}
