/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import de.schlichtherle.truevfs.kernel.CacheEntry;
import static de.schlichtherle.truevfs.kernel.CacheEntry.Strategy.WRITE_BACK;
import de.schlichtherle.truevfs.kernel.ClutchInputSocket;
import de.schlichtherle.truevfs.kernel.ClutchOutputSocket;
import de.schlichtherle.truevfs.kernel.NeedsSyncException;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static net.truevfs.kernel.FsAccessOption.*;
import static net.truevfs.kernel.FsSyncOption.ABORT_CHANGES;
import static net.truevfs.kernel.FsSyncOption.CLEAR_CACHE;
import static net.truevfs.kernel.FsSyncOptions.SYNC;
import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Entry.Type;
import static net.truevfs.kernel.cio.Entry.Type.FILE;
import net.truevfs.kernel.cio.*;
import net.truevfs.kernel.io.DecoratingInputStream;
import net.truevfs.kernel.io.DecoratingOutputStream;
import net.truevfs.kernel.io.DecoratingSeekableChannel;
import net.truevfs.kernel.util.BitField;

/**
 * A selective cache for file system entries.
 * Decorating a file system controller with this class has the following
 * effects:
 * <ul>
 * <li>Caching and buffering for an entry needs to get activated by using the
 *     method
 *     {@link #input input lazySocket} with the input option
 *     {@link FsAccessOption#CACHE} or the method
 *     {@link #output output lazySocket} with the output option
 *     {@link FsAccessOption#CACHE}.
 * <li>Unless a write operation succeeds, upon each read operation the entry
 *     data gets copied from the backing store for buffering purposes only.
 * <li>Upon a successful write operation, the entry data gets cached for
 *     subsequent read operations until the file system gets
 *     {@link #sync synced} again.
 * <li>Entry data written to the cache is not written to the backing store
 *     until the file system gets {@link #sync synced} - this is a
 *     <i>write back</i> strategy.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the copied
 *     entry data.
 * </ul>
 * <p>
 * <strong>TO THE FUTURE ME:</strong>
 * FOR TRUEZIP 7.5, IT TOOK ME TWO MONTHS OF CONSECUTIVE CODING, TESTING,
 * DEBUGGING, ANALYSIS AND SWEATING TO GET THIS DAMN BEAST WORKING STRAIGHT!
 * DON'T EVEN THINK YOU COULD CHANGE A SINGLE CHARACTER IN THIS CODE AND EASILY
 * GET AWAY WITH IT!
 * <strong>YOU HAVE BEEN WARNED!</strong>
 * <p>
 * Well, if you really feel like changing something, run the integration test
 * suite at least ten times to make sure your changes really work - I mean it!
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class CacheController
extends DecoratingLockModelController<FsController<? extends LockModel>> {

    private static final Logger logger = Logger.getLogger(
            CacheController.class.getName(),
            CacheController.class.getName());

    private final IOPool<?> pool;

    private final Map<FsEntryName, EntryCache> caches = new HashMap<>();

    /**
     * Constructs a new cache controller.
     *
     * @param pool the pool of I/O buffers to hold the cached entry contents.
     * @param controller the decorated file system controller.
     */
    CacheController(
            final IOPool<?> pool,
            final FsController<? extends LockModel> controller) {
        super(controller);
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public InputSocket<?> input(
            final BitField<FsAccessOption> options,
            final FsEntryName name) {
        /** This class requires ON-DEMAND LOOKUP of its delegate lazySocket! */
        class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<?> socket() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(CACHE))
                        return controller.input(options, name);
                    //checkWriteLockedByCurrentThread();
                    cache = new EntryCache(name);
                }
                return cache.input(options);
            }
        }
        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive!
    public OutputSocket<?> output(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final @CheckForNull Entry template) {
        /** This class requires ON-DEMAND LOOKUP of its delegate lazySocket! */
        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<?> socket() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(CACHE))
                        return controller.output(options, name, template);
                    //checkWriteLockedByCurrentThread();
                    cache = new EntryCache(name);
                }
                return cache.output(options, template);
            }
        }
        return new Output();
    }

    @Override
    public void mknod(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Type type,
            final @CheckForNull Entry template)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        controller.mknod(options, name, type, template);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.release();
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        controller.unlink(options, name);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.release();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        assert isWriteLockedByCurrentThread();
        NeedsSyncException preSyncEx;
        do {
            preSyncEx = null;
            try {
                preSync(options);
            } catch (final NeedsSyncException invalidState) {
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
            // TODO: Consume FsSyncOption.CLEAR_CACHE and release a flag in
            // the model instead.
            controller.sync(options/*.clear(CLEAR_CACHE)*/);
        } while (null != preSyncEx);
    }

    private void preSync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        if (0 >= caches.size())
            return;
        final boolean flush = !options.get(ABORT_CHANGES);
        boolean release = !flush || options.get(CLEAR_CACHE);
        assert flush || release;
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        for (   final Iterator<EntryCache> i = caches.values().iterator();
                i.hasNext(); ) {
            final EntryCache cache = i.next();
            try {
                if (flush) {
                    try {
                        cache.flush();
                    } catch (final IOException ex) {
                        throw builder.fail(new FsSyncException(getModel(), ex));
                    }
                }
            } catch (final Throwable ex) {
                release = false;
                throw ex;
            } finally {
                if (release) {
                    i.remove();
                    try {
                        cache.release();
                    } catch (final IOException ex) {
                        builder.warn(new FsSyncWarningException(getModel(), ex));
                    }
                }
            }
        }
        builder.check();
    }

    /** A cache for the contents of an individual archive entry. */
    @Immutable
    private final class EntryCache {
        final FsEntryName name;
        final CacheEntry cache;

        EntryCache(final FsEntryName name) {
            this.name = name;
            this.cache = WRITE_BACK.newCache(pool);
        }

        void flush() throws IOException {
            cache.flush();
        }

        void release() throws IOException {
            cache.release();
        }

        void register() {
            caches.put(name, this);
        }

        InputSocket<?> input(final BitField<FsAccessOption> options) {
            /**
             * This class requires LAZY INITIALIZATION of its channel, but NO
             * automatic decoupling on exceptions!
             */
            @NotThreadSafe
            final class Input extends ClutchInputSocket<Entry> {
                private final BitField<FsAccessOption> o = options.clear(CACHE); // consume

                @Override
                protected InputSocket<? extends Entry> lazySocket() {
                    return controller.input(o, name);
                }

                @Override
                public Entry localTarget() throws IOException {
                    // Bypass the super class implementation to keep the
                    // lazySocket even upon an exception!
                    return boundSocket().localTarget();
                }

                @Override
                public InputStream stream() throws IOException {
                    assert isWriteLockedByCurrentThread();

                    final class Stream extends DecoratingInputStream {
                        @CreatesObligation
                        Stream() throws IOException {
                            // Bypass the super class implementation to keep the
                            // channel even upon an exception!
                            //super(Input.super.stream());
                            super(boundSocket().stream());
                            assert getModel().isTouched();
                        }

                        @Override
                        @DischargesObligation
                        public void close() throws IOException {
                            assert isWriteLockedByCurrentThread();
                            in.close();
                            register();
                        }
                    }
                    return new Stream();
                }

                @Override
                public SeekableByteChannel channel(){
                    throw new AssertionError();
                }
            }
            return cache.configure(new Input()).input();
        }

        OutputSocket<?> output( final BitField<FsAccessOption> options,
                                final @CheckForNull Entry template) {
            /**
             * This class requires LAZY INITIALIZATION of its channel, but NO
             * automatic decoupling on exceptions!
             */
            @NotThreadSafe
            final class Output extends ClutchOutputSocket<Entry> {
                final BitField<FsAccessOption> o = options.clear(CACHE); // consume

                @Override
                protected OutputSocket<? extends Entry> lazySocket() {
                    return cache
                            .configure(controller.output(
                                o.clear(EXCLUSIVE), name, template))
                            .output();
                }

                @Override
                public Entry localTarget() throws IOException {
                    // Bypass the super class implementation to keep the
                    // lazySocket even upon an exception!
                    return boundSocket().localTarget();
                }

                @Override
                public OutputStream stream() throws IOException {
                    assert isWriteLockedByCurrentThread();
                    preOutput();

                    final class Stream extends DecoratingOutputStream {
                        @CreatesObligation
                        Stream() throws IOException {
                            // Bypass the super class implementation to keep the
                            // lazySocket even upon an exception!
                            //super(Output.super.stream());
                            super(boundSocket().stream());
                            register();
                        }

                        @Override
                        @DischargesObligation
                        public void close() throws IOException {
                            assert isWriteLockedByCurrentThread();
                            out.close();
                            postOutput();
                        }
                    }
                    return new Stream();
                }

                @Override
                public SeekableByteChannel channel() throws IOException {
                    assert isWriteLockedByCurrentThread();
                    preOutput();

                    final class Channel extends DecoratingSeekableChannel {
                        @CreatesObligation
                        Channel() throws IOException {
                            // Bypass the super class implementation to keep the
                            // lazySocket even upon an exception!
                            //super(Output.super.channel());
                            super(boundSocket().channel());
                            register();
                        }

                        @Override
                        @DischargesObligation
                        public void close() throws IOException {
                            assert isWriteLockedByCurrentThread();
                            channel.close();
                            postOutput();
                        }
                    }
                    return new Channel();
                }

                void preOutput() throws IOException {
                    mknod(o, template);
                }

                void postOutput() throws IOException {
                    mknod(  o.clear(EXCLUSIVE),
                            null != template ? template : cache);
                    register();
                }

                void mknod( final BitField<FsAccessOption> options,
                            final @CheckForNull Entry template)
                throws IOException {
                    BitField<FsAccessOption> mknodOpts = options;
                    while (true) {
                        try {
                            controller.mknod(mknodOpts, name, FILE, template);
                            break;
                        } catch (final NeedsSyncException mknodEx) {
                            // In this context, this exception means that the entry
                            // has already been written to the output archive for
                            // the target archive file.

                            // Pass on the exception if there is no means to
                            // resolve the issue locally, that is if we were asked
                            // to create the entry exclusively or this is a
                            // non-recursive file system operation.
                            if (mknodOpts.get(EXCLUSIVE))
                                throw mknodEx;
                            final BitField<FsSyncOption> syncOpts = SyncController.modify(SYNC);
                            if (SYNC == syncOpts)
                                throw mknodEx;

                            // Try to resolve the issue locally.
                            // Even if we were asked to create the entry
                            // EXCLUSIVEly, first we must try to get the cache in
                            // sync() with the virtual file system again and retry
                            // the mknod().
                            try {
                                controller.sync(syncOpts);
                                //continue; // sync() succeeded, now repeat mknod()
                            } catch (final FsSyncException syncEx) {
                                syncEx.addSuppressed(mknodEx);

                                // sync() failed, maybe just because the current
                                // thread has already acquired some open I/O
                                // resources for the same target archive file, e.g.
                                // an input stream for a copy operation and this
                                // is an artifact of an attempt to acquire the
                                // output stream for a child file system.
                                if (!(syncEx.getCause() instanceof FsResourceOpenException)) {
                                    // Too bad, sync() failed because of a more
                                    // serious issue than just some open resources.
                                    // Let's rethrow the sync exception.
                                    throw syncEx;
                                }

                                // OK, we couldn't sync() because the current
                                // thread has acquired open I/O resources for the
                                // same target archive file.
                                // Normally, we would be expected to rethrow the
                                // mknod exception to trigger another sync(), but
                                // this would fail for the same reason und create
                                // an endless loop, so we can't do this.
                                //throw mknodEx;

                                // Dito for mapping the exception.
                                //throw FsNeedsLockRetryException.get(getModel());

                                // Check if we can retry the mknod with GROW set.
                                mknodOpts = mknodOpts.set(GROW);
                                if (mknodOpts == options) {
                                    // Finally, the mknod failed because the entry
                                    // has already been output to the target archive
                                    // file - so what?!
                                    // This should mark only a volatile issue because
                                    // the next sync() will sort it out once all the
                                    // I/O resources have been closed.
                                    // Let's log the sync exception - mind that it has
                                    // suppressed the mknod exception - and continue
                                    // anyway...
                                    logger.log(Level.FINE, "ignoring", syncEx);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return new Output();
        }
    } // EntryCache
}
