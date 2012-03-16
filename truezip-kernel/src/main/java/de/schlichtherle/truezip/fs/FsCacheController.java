/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import static de.schlichtherle.truezip.fs.FsOutputOption.EXCLUSIVE;
import static de.schlichtherle.truezip.fs.FsSyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.fs.FsSyncOption.CLEAR_CACHE;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import static de.schlichtherle.truezip.socket.IOCache.Strategy.WRITE_BACK;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A selective cache for file system entry contents.
 * Decorating a concurrent file system controller with this class has the
 * following effects:
 * <ul>
 * <li>Caching and buffering needs to be activated by using the method
 *     {@link #getInputSocket input socket} with the input option
 *     {@link FsInputOption#CACHE} or the method
 *     {@link #getOutputSocket output socket} with the output option
 *     {@link FsOutputOption#CACHE}.
 * <li>Unless a write operation succeeds, upon each read operation the entry
 *     data gets copied from the backing store for buffering purposes only.
 * <li>Upon a successful write operation, the entry data gets cached for
 *     subsequent read operations until the file system gets
 *     {@link #sync synced}.
 * <li>Entry data written to the cache is not written to the backing store
 *     until the file system gets {@link #sync synced} - this is a
 *     <i>write back</i> strategy.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the copied
 *     entry data.
 * </ul>
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class FsCacheController
extends FsLockModelDecoratingController<FsController<? extends FsLockModel>> {

    private static final Logger logger = Logger.getLogger(
            FsCacheController.class.getName(),
            FsCacheController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private final IOPool<?> pool;

    private final Map<FsEntryName, EntryCache>
            caches = new HashMap<FsEntryName, EntryCache>();

    /**
     * Constructs a new file system cache controller.
     *
     * @param controller the decorated file system controller.
     * @param pool the pool of I/O buffers to hold the cached entry contents.
     */
    public FsCacheController(
            final FsController<? extends FsLockModel> controller,
                                final IOPool<?> pool) {
        super(controller);
        if (null == (this.pool = pool))
            throw new NullPointerException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<?> getDelegate() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(FsInputOption.CACHE))
                        return delegate.getInputSocket(name, options);
                    cache = new EntryCache(name);
                }
                return cache.getInputSocket(options);
            }
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive!
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<?> getDelegate() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(FsOutputOption.CACHE))
                        return delegate.getOutputSocket(name, options, template);
                    cache = new EntryCache(name);
                }
                return cache.getOutputSocket(options, template);
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final @CheckForNull Entry template)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        delegate.mknod(name, type, options, template);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        delegate.unlink(name, options);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        FsNeedsSyncException preSyncEx;
        do {
            preSyncEx = null;
            try {
                preSync(options, handler);
            } catch (final FsNeedsSyncException invalidState) {
                // The target archive controller is in an invalid state because
                // it reports to need a sync() while the current thread is
                // actually doing a sync().
                // This is expected to be a volatile event which may have been
                // caused by the following scenario:
                // Another thread attempted to sync() the nested target archive
                // file but initially failed because the parent file system
                // controller has thrown an FsNeedsLockRetryException when
                // trying to close() the input or output resources for the
                // target archive file.
                // The other thread has then released all its file system write
                // locks and is now retrying the operation but lost the race
                // for the file system write lock against this thread which has
                // now detected the invalid state.
                
                // TODO: This is not the only possible scenario, so this
                // assertion may fail!
                //assert null != getParent().getParent() : invalidState;

                // In an attempt to recover from this invalid state, the
                // current thread could just step back in order to give the
                // other thread a chance to complete its sync().
                //throw FsNeedsLockRetryException.get(getModel());

                // However, this would unnecessarily defer the current thread
                // and might result in yet another thread to discover the
                // invalid state, which reduces the overall performance.
                // So instead, the current thread will now attempt to resolve
                // the invalid state by sync()ing the target archive controller
                // before preSync()ing the cache again.
                logger.log(Level.FINE, "recovering", invalidState);
                preSyncEx = invalidState; // trigger another iteration
            }
            // TODO: Consume FsSyncOption.CLEAR_CACHE and clear a flag in
            // the model instead.
            delegate.sync(options/*.clear(CLEAR_CACHE)*/, handler);
        } while (null != preSyncEx);
    }

    private <X extends IOException> void
    preSync(final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        assert isWriteLockedByCurrentThread();
        if (0 >= caches.size())
            return;
        final boolean flush = !options.get(ABORT_CHANGES);
        boolean clear = !flush || options.get(CLEAR_CACHE);
        assert flush || clear;
        for (   final Iterator<EntryCache> i = caches.values().iterator();
                i.hasNext(); ) {
            final EntryCache cache = i.next();
            try {
                if (flush) {
                    try {
                        cache.flush();
                    } catch (final FsControllerException ex) {
                        clear = false;
                        throw ex;
                    } catch (final IOException ex) {
                        throw handler.fail(new FsSyncException(getModel(), ex));
                    }
                }
            } finally {
                if (clear) {
                    i.remove();
                    try {
                        cache.clear();
                    } catch (final FsControllerException ex) {
                        assert false;
                        throw ex;
                    } catch (final IOException ex) {
                        handler.warn(new FsSyncWarningException(getModel(), ex));
                    }
                }
            }
        }
    }

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryCache cache,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return cache.new Nio2Output(options, template);
            }
        },
        
        OIO() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryCache cache,
                    BitField<FsOutputOption> options,
                    @CheckForNull Entry template) {
                return cache.new Output(options, template);
            }
        };

        abstract OutputSocket<?> newOutputSocket(
                EntryCache cache,
                BitField<FsOutputOption> options,
                @CheckForNull Entry template);
    } // SocketFactory

    /** A cache for the contents of an individual archive entry. */
    @Immutable
    private final class EntryCache {
        final FsEntryName name;
        final IOCache cache;

        EntryCache(final FsEntryName name) {
            this.name = name;
            this.cache = WRITE_BACK.newCache(FsCacheController.this.pool);
        }

        InputSocket<?> getInputSocket(BitField<FsInputOption> options) {
            return cache.configure(new Input(options)).getInputSocket();
        }

        OutputSocket<?> getOutputSocket(BitField<FsOutputOption> options,
                                        @CheckForNull Entry template) {
            return SOCKET_FACTORY.newOutputSocket(this, options, template);
        }

        void flush() throws IOException {
            cache.flush();
        }

        void clear() throws IOException {
            cache.clear();
        }

        /**
         * This class needs the lazy initialization and exception handling
         * provided by its super class.
         */
        @Immutable
        final class Input extends ClutchInputSocket<Entry> {
            final BitField<FsInputOption> options;

            Input(final BitField<FsInputOption> options) {
                this.options = options.clear(FsInputOption.CACHE); // consume
            }

            @Override
            protected InputSocket<? extends Entry> getLazyDelegate()
            throws IOException {
                return delegate.getInputSocket(name, options);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel(){
                throw new UnsupportedOperationException();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                class Stream extends DecoratingInputStream {
                    @CreatesObligation
                    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                    Stream() throws IOException {
                        super(Input.super.newInputStream());
                        assert isTouched();
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                        assert isWriteLockedByCurrentThread();
                        caches.put(name, EntryCache.this);
                    }
                } // Stream

                return new Stream();
            }
        } // Input

        @Immutable
        final class Nio2Output extends Output {
            Nio2Output( final BitField<FsOutputOption> options,
                        final @CheckForNull Entry template) {
                super(options, template);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                preOutput();

                class Channel extends DecoratingSeekableByteChannel {
                    @CreatesObligation
                    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                    Channel() throws IOException {
                        super(Nio2Output.super.newSeekableByteChannel());
                        assert isWriteLockedByCurrentThread();
                        caches.put(name, EntryCache.this);
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                        postOutput();
                    }
                } // Channel

                return new Channel();
            }
        } // Nio2Output

        /**
         * This class needs the lazy initialization and exception handling
         * provided by its super class.
         */
        @Immutable
        class Output extends ClutchOutputSocket<Entry> {
            final BitField<FsOutputOption> options;
            final @CheckForNull Entry template;

            Output( final BitField<FsOutputOption> options,
                    final @CheckForNull Entry template) {
                this.options = options.clear(FsOutputOption.CACHE); // consume
                this.template = template;
            }

            @Override
            protected OutputSocket<? extends Entry> getLazyDelegate()
            throws IOException {
                return cache.configure( delegate.getOutputSocket(
                                            name,
                                            options.clear(EXCLUSIVE),
                                            template))
                            .getOutputSocket();
            }

            @Override
            public final OutputStream newOutputStream() throws IOException {
                preOutput();

                class Stream extends DecoratingOutputStream {
                    @CreatesObligation
                    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                    Stream() throws IOException {
                        super(Output.super.newOutputStream());
                        assert isWriteLockedByCurrentThread();
                        caches.put(name, EntryCache.this);
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                        postOutput();
                    }
                } // Stream

                return new Stream();
            }

            void preOutput() throws IOException {
                mknod(options, template);
            }

            void postOutput() throws IOException {
                mknod(  options.clear(EXCLUSIVE),
                        null != template ? template : cache.getEntry());
                assert isWriteLockedByCurrentThread();
                caches.put(name, EntryCache.this); // may re-install after clear
            }

            void mknod( final BitField<FsOutputOption> options,
                        final @CheckForNull Entry template)
            throws IOException {
                while (true) {
                    try {
                        delegate.mknod(name, FILE, options, template);
                        break;
                    } catch (final FsNeedsSyncException mknodEx) {
                        // In this context, this exception means that the entry
                        // has already been written to the output archive for
                        // the target archive file.

                        // Recovering from this exception may fail because this
                        // might be an attempt to acquire an output stream for
                        // a copy operation and the input stream may have been
                        // acquired already and accessing the same archive file.
                        // The sync() would then fail with an FsSyncException
                        // because the target archive file is busy with this
                        // input stream.
                        try {
                            delegate.sync(FsSyncOptions.SYNC);
                            continue; // recovery succeeded, now repeat mknod
                        } catch (final FsSyncException syncEx) {
                            if (!(syncEx.getCause() instanceof FsOpenIOResourcesException)) {
                                // This indicates an issue which is more
                                // serious than just some open resources, so
                                // pass it on.
                                throw syncEx;
                            }
                            // We couldn't recover the mknod failure because
                            // the current thread is holding open I/O resources.

                            // Passing this exception would trigger another sync()
                            // which may fail for the same reason und thus create
                            // an endless loop
                            //throw mknodEx;

                            // Dito for mapping the exception.
                            //throw FsNeedsLockRetryException.get(getModel());

                            // So we can just log this issue.
                            // It's expected to be volatile and should vanish
                            // upon the next sync().
                            logger.log(options.get(EXCLUSIVE)   ? Level.WARNING
                                                                : Level.INFO,
                                    "ignoring", mknodEx);
                            break;
                        }
                    }
                }
            }
        } // Output
    } // EntryCache
}
