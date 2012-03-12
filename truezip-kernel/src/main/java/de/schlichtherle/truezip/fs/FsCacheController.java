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
 * Implements a combined caching and buffering strategy for entry data.
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
extends FsLockModelDecoratingController<
        FsController<? extends FsLockModel>> {

    private static final Logger logger = Logger.getLogger(
            FsCacheController.class.getName(),
            FsCacheController.class.getName());

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private final IOPool<?> pool;

    // TODO: Consider using a ConcurrentMap to support concurrent access just
    // protected by a read lock.
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
        if (null == pool)
            throw new NullPointerException();
        this.pool = pool;
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
                        return FsCacheController.this.delegate
                                .getInputSocket(name, options);
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
                        return FsCacheController.this.delegate
                                .getOutputSocket(name, options, template);
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
        final EntryCache cache = caches.get(name);
        delegate.mknod(name, type, options, template);
        if (null != cache) {
            caches.remove(name);
            cache.clear();
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        final EntryCache cache = caches.get(name);
        delegate.unlink(name, options);
        if (null != cache) {
            caches.remove(name);
            cache.clear();
        }
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        preSync(options, handler);
        // TODO: Consume FsSyncOption.CLEAR_CACHE and clear a flag in the model
        // instead.
        delegate.sync(options/*.clear(CLEAR_CACHE)*/, handler);
    }

    private <X extends IOException> void
    preSync(final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        assert isWriteLockedByCurrentThread();
        if (0 >= caches.size())
            return;
        final boolean flush = !options.get(ABORT_CHANGES);
        final boolean clear = !flush || options.get(CLEAR_CACHE);
        assert flush || clear;
        final Iterator<EntryCache> i = caches.values().iterator();
        while (i.hasNext()) {
            final EntryCache cache = i.next();
            try {
                if (flush) {
                    try {
                        cache.flush();
                    } catch (FsControllerException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        throw handler.fail(new FsSyncException(getModel(), ex));
                    }
                }
            } finally {
                if (clear) {
                    i.remove();
                    try {
                        cache.clear();
                    } catch (FsControllerException ex) {
                        throw ex;
                    } catch (IOException ex) {
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
            try {
                cache.flush();
            } catch (FsNeedsSyncException alreadyPresent) {
                logger.log(Level.FINER,
                        FsNeedsSyncException.class.getSimpleName(),
                        alreadyPresent);
            }
        }

        void clear() throws IOException {
            cache.clear();
        }

        /**
         * This class needs the lazy initialization and exception handling
         * provided by its super class.
         */
        final class Input extends ProxyInputSocket<Entry> {
            final BitField<FsInputOption> options;

            Input(final BitField<FsInputOption> options) {
                this.options = options.clear(FsInputOption.CACHE); // consume
            }

            @Override
            protected InputSocket<? extends Entry> getLazyDelegate()
            throws IOException {
                return FsCacheController.this.delegate.getInputSocket(
                        EntryCache.this.name, options);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel(){
                throw new AssertionError();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() {
                throw new AssertionError();
            }

            @Override
            public InputStream newInputStream() throws IOException {
                assert isWriteLockedByCurrentThread();

                class Stream extends DecoratingInputStream {
                    @CreatesObligation
                    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                    Stream() throws IOException {
                        super(Input.super.newInputStream());
                        assert isTouched();
                    }

                    @Override
                    public void close() throws IOException {
                        assert isWriteLockedByCurrentThread();
                        delegate.close();
                        caches.put(EntryCache.this.name, EntryCache.this);
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
                assert isWriteLockedByCurrentThread();
                pre();

                class Channel extends DecoratingSeekableByteChannel {
                    @CreatesObligation
                    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                    Channel() throws IOException {
                        super(Nio2Output.super.newSeekableByteChannel());
                        FsCacheController.this.caches.put(
                                EntryCache.this.name,
                                EntryCache.this);
                    }

                    @Override
                    public void close() throws IOException {
                        assert isWriteLockedByCurrentThread();
                        delegate.close();
                        post();
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
        class Output extends ProxyOutputSocket<Entry> {
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
                return EntryCache.this.cache.configure(
                            FsCacheController.this.delegate.getOutputSocket(
                                EntryCache.this.name,
                                options.clear(EXCLUSIVE),
                                template))
                        .getOutputSocket();
            }

            @Override
            public final OutputStream newOutputStream() throws IOException {
                assert isWriteLockedByCurrentThread();
                pre();

                class Stream extends DecoratingOutputStream {
                    @CreatesObligation
                    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                    Stream() throws IOException {
                        super(Output.super.newOutputStream());
                        FsCacheController.this.caches.put(
                                EntryCache.this.name,
                                EntryCache.this);
                    }

                    @Override
                    public void close() throws IOException {
                        assert isWriteLockedByCurrentThread();
                        delegate.close();
                        post();
                    }
                } // Stream

                return new Stream();
            }

            void pre() throws IOException {
                try {
                    FsCacheController.this.delegate.mknod(
                            EntryCache.this.name,
                            FILE,
                            options,
                            template);
                } catch (FsNeedsSyncException alreadyPresent) {
                    if (options.get(EXCLUSIVE))
                        throw alreadyPresent;
                    logger.log(Level.FINER,
                            FsNeedsSyncException.class.getSimpleName(),
                            alreadyPresent);
                }
            }

            void post() throws IOException {
                try {
                    FsCacheController.this.delegate.mknod(
                            EntryCache.this.name,
                            FILE,
                            options.clear(EXCLUSIVE),
                            null != template
                                ? template
                                : EntryCache.this.cache.getEntry());
                } catch (FsNeedsSyncException alreadyPresent) {
                    logger.log(Level.FINER,
                            FsNeedsSyncException.class.getSimpleName(),
                            alreadyPresent);
                }
            }
        } // Output
    } // EntryCache
}
