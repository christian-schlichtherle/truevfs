/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import lombok.val;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DecoratingSeekableChannel;
import net.java.truecommons.logging.LocalizedLogger;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;
import org.slf4j.Logger;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.java.truecommons.cio.Entry.Type.FILE;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;
import static net.java.truevfs.kernel.spec.FsSyncOption.ABORT_CHANGES;
import static net.java.truevfs.kernel.spec.FsSyncOption.CLEAR_CACHE;
import static net.java.truevfs.kernel.spec.FsSyncOptions.SYNC;

/**
 * A selective cache for file system entries.
 * Decorating a file system controller with this class has the following effects:
 * <p>
 * - Caching and buffering for an entry needs to get activated by using the methods {@code input} or {@code output} with
 * the access option {@link net.java.truevfs.kernel.spec.FsAccessOption#CACHE}.
 * - Unless a write operation succeeds, upon each read operation the entry data gets copied from the backing store for
 * buffering purposes only.
 * - Upon a successful write operation, the entry data gets cached for subsequent read operations until the file system
 * gets {@code sync}ed again.
 * - Entry data written to the cache is not written to the backing store until the file system gets `sync`ed - this is
 * a "write back" strategy.
 * - As a side effect, caching decouples the underlying storage from its clients, allowing it to create, read, update or
 * delete the entry data while some clients are still busy on reading or writing the copied entry data.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class CacheController<E extends FsArchiveEntry> extends DecoratingArchiveController<E> {

    private static final Logger logger = new LocalizedLogger(CacheController.class);

    private final Map<FsNodeName, EntryCache> caches = new HashMap<>();

    private final IoBufferPool pool;

    CacheController(IoBufferPool pool, ArchiveController<E> controller) {
        super(controller);
        this.pool = pool;
    }

    @Override
    public InputSocket<? extends Entry> input(BitField<FsAccessOption> options, FsNodeName name) {
        // This class requires ON-DEMAND LOOKUP of its delegate socket!
        return new DelegatingInputSocket<Entry>() {

            @Override
            public InputSocket<? extends Entry> socket() {
                assert writeLockedByCurrentThread();

                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(CACHE)) {
                        return controller.input(options, name);
                    }
                    cache = new EntryCache(name);
                }
                return cache.input(options);
            }
        };
    }

    @Override
    public OutputSocket<? extends Entry> output(BitField<FsAccessOption> options, FsNodeName name, Optional<Entry> template) {
        // This class requires ON-DEMAND LOOKUP of its delegate socket!
        return new DelegatingOutputSocket<Entry>() {

            @Override
            public OutputSocket<? extends Entry> socket() {
                assert writeLockedByCurrentThread();

                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(CACHE)) {
                        return controller.output(options, name, template);
                    }
                    cache = new EntryCache(name);
                }
                return cache.output(options, template);
            }
        };
    }

    @Override
    public void make(final BitField<FsAccessOption> options, final FsNodeName name, final Entry.Type type, final Optional<Entry> template) throws IOException {
        assert writeLockedByCurrentThread();

        controller.make(options, name, type, template);
        val cache = caches.remove(name);
        if (null != cache) {
            cache.clear();
        }
    }

    @Override
    public void unlink(final BitField<FsAccessOption> options, final FsNodeName name) throws IOException {
        assert writeLockedByCurrentThread();

        controller.unlink(options, name);
        val cache = caches.remove(name);
        if (null != cache) {
            cache.clear();
        }
    }

    @Override
    public void sync(final BitField<FsSyncOption> options) throws FsSyncException {
        assert writeLockedByCurrentThread();
        assert !readLockedByCurrentThread();

        syncCacheEntries(options);
        controller.sync(options.clear(CLEAR_CACHE));
        if (caches.isEmpty()) {
            setMounted(false);
        }
    }

    private void syncCacheEntries(final BitField<FsSyncOption> options) throws FsSyncException {
        // HC SVNT DRACONES!
        if (0 >= caches.size()) {
            return;
        }
        val flush = !options.get(ABORT_CHANGES);
        val clear = !flush || options.get(CLEAR_CACHE);
        val builder = new FsSyncExceptionBuilder();
        val it = caches.values().iterator();
        while (it.hasNext()) {
            val cache = it.next();
            if (flush) {
                try {
                    cache.flush();
                } catch (IOException e) {
                    throw builder.fail(new FsSyncException(getMountPoint(), e));
                }
            }
            if (clear) {
                it.remove();
                try {
                    cache.clear();
                } catch (IOException e) {
                    builder.warn(new FsSyncWarningException(getMountPoint(), e));
                }
            }
        }
        builder.check();
    }

    /**
     * A cache for the contents of an individual archive entry.
     */
    private final class EntryCache {

        final CacheEntry cache = CacheEntry.Strategy.WriteBack.newCacheEntry(pool);
        final FsNodeName name;

        EntryCache(final FsNodeName name) {
            this.name = name;
        }

        void flush() throws IOException {
            cache.flush();
        }

        void clear() throws IOException {
            cache.release();
        }

        void register() {
            caches.put(name, this);
        }

        InputSocket<? extends Entry> input(final BitField<FsAccessOption> options) {

            final class Input extends DelegatingInputSocket<Entry> {

                final BitField<FsAccessOption> _options = options.clear(CACHE); // consume

                final InputSocket<? extends Entry> socket = controller.input(_options, name);

                @Override
                protected InputSocket<? extends Entry> socket() throws IOException {
                    return socket;
                }

                @Override
                public InputStream stream(final OutputSocket<? extends Entry> peer) throws IOException {
                    assert writeLockedByCurrentThread();
                    return new DecoratingInputStream(socket().stream(peer)) {

                        {
                            assert isMounted();
                        }

                        @Override
                        public void close() throws IOException {
                            assert writeLockedByCurrentThread();
                            in.close();
                            register();
                        }
                    };
                }

                @Override
                public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                    throw new AssertionError();
                }
            }

            return cache.configure(new Input()).input();
        }

        OutputSocket<? extends Entry> output(final BitField<FsAccessOption> options, final Optional<Entry> template) {

            // This class requires lazy initialization of its channel, but no automatic decoupling on exceptions!
            final class Output extends DelegatingOutputSocket<Entry> {

                final BitField<FsAccessOption> _options = options.clear(CACHE); // consume

                final OutputSocket<? extends Entry> socket = cache
                        .configure(controller.output(_options.clear(EXCLUSIVE), name, template))
                        .output();

                @Override
                protected OutputSocket<? extends Entry> socket() throws IOException {
                    return socket;
                }

                @Override
                public OutputStream stream(final InputSocket<? extends Entry> peer) throws IOException {
                    assert writeLockedByCurrentThread();
                    preOutput();

                    return new DecoratingOutputStream(socket().stream(peer)) {

                        {
                            register();
                        }

                        @Override
                        public void close() throws IOException {
                            assert writeLockedByCurrentThread();
                            out.close();
                            postOutput();
                        }
                    };
                }

                @Override
                public SeekableByteChannel channel(final InputSocket<? extends Entry> peer) throws IOException {
                    assert writeLockedByCurrentThread();
                    preOutput();

                    return new DecoratingSeekableChannel(socket().channel(peer)) {

                        {
                            register();
                        }

                        @Override
                        public void close() throws IOException {
                            assert writeLockedByCurrentThread();
                            channel.close();
                            postOutput();
                        }
                    };
                }

                void preOutput() throws IOException {
                    make(_options, template);
                }

                void postOutput() throws IOException {
                    make(_options.clear(EXCLUSIVE), template.isPresent() ? template : Optional.of(cache));
                    register();
                }

                void make(final BitField<FsAccessOption> options, final Optional<Entry> template) throws IOException {
                    BitField<FsAccessOption> makeOpts = options;
                    while (true) {
                        try {
                            controller.make(makeOpts, name, FILE, template);
                            return;
                        } catch (final NeedsSyncException makeEx) {
                            // In this context, this exception means that the entry has already been written to the
                            // output archive for the target archive file.

                            // Pass on the exception if there is no means to resolve the issue locally, that is if we
                            // were asked to create the entry exclusively or this is a non-recursive file system
                            // operation.
                            if (makeOpts.get(EXCLUSIVE)) {
                                throw makeEx;
                            }
                            val syncOpts = SyncController.modify(SYNC);
                            if (SYNC == syncOpts) {
                                throw makeEx;
                            }

                            // Try to resolve the issue locally.
                            // Even if we were asked to create the entry EXCLUSIVEly, we first need to try to get the
                            // cache in sync() with the virtual file system again and retry the make().
                            try {
                                controller.sync(syncOpts);
                                // sync() succeeded, now repeat the make().
                            } catch (final FsSyncException syncEx) {
                                syncEx.addSuppressed(makeEx);

                                // sync() failed, maybe just because the current thread has already acquired some open
                                // I/O resources for the same target archive file, e.g. an input stream for a copy
                                // operation and this is an artifact of an attempt to acquire the output stream for a
                                // child file system.
                                if (syncEx.getCause() instanceof FsOpenResourceException) {
                                    // OK, we couldn't sync() because the current thread has acquired open I/O resources
                                    // for the same target archive file.
                                    // Normally, we would be expected to rethrow the make exception to trigger another
                                    // sync(), but this would fail for the same reason und create an endless loop, so we
                                    // can't do this.
                                    //throw mknodEx;

                                    // Dito for mapping the exception.
                                    //throw FsNeedsLockRetryException.get(getModel());

                                    // Check if we can retry the make with GROW set.
                                    val oldMknodOpts = makeOpts;
                                    makeOpts = oldMknodOpts.set(GROW);
                                    if (makeOpts == oldMknodOpts) {
                                        // Finally, the make failed because the entry has already been output to the
                                        // target archive file - so what?!
                                        // This should mark only a volatile issue because the next sync() will sort it
                                        // out once all the I/O resources have been closed.
                                        // Let's log the sync exception - mind that it has suppressed the make exception
                                        // - and continue anyway...
                                        logger.debug("ignoring", syncEx);
                                        return;
                                    }
                                } else {
                                    // Too bad, sync() failed because of a more serious issue than just some open
                                    // resources.
                                    // Let's rethrow the sync exception.
                                    throw syncEx;
                                }
                            }
                        }
                    }
                }
            }

            return new Output();
        }
    }
}
