/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsControlFlowIOException;
import static de.schlichtherle.truezip.kernel.Cache.Strategy.WRITE_BACK;
import de.truezip.kernel.FsResourceOpenException;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncWarningException;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.FsAccessOption;
import static de.truezip.kernel.FsAccessOption.EXCLUSIVE;
import de.truezip.kernel.FsSyncOption;
import static de.truezip.kernel.FsSyncOption.ABORT_CHANGES;
import static de.truezip.kernel.FsSyncOption.CLEAR_CACHE;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
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
 * A selective cache for file system entries.
 * Decorating a file system controller with this class has the following
 * effects:
 * <ul>
 * <li>Caching and buffering for an entry needs to get activated by using the
 *     method
 *     {@link #getInputSocket input socket} with the input option
 *     {@link FsAccessOption#CACHE} or the method
 *     {@link #getOutputSocket output socket} with the output option
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
extends DecoratingLockModelController<SyncDecoratingController<? extends LockModel, ?>> {

    private static final Logger logger = Logger.getLogger(
            CacheController.class.getName(),
            CacheController.class.getName());

    private final IOPool<?> pool;

    private final Map<FsEntryName, EntryCache> caches = new HashMap<>();

    /**
     * Constructs a new file system cache controller.
     *
     * @param controller the decorated file system controller.
     * @param pool the pool of I/O buffers to hold the cached entry contents.
     */
    CacheController(  final SyncDecoratingController<? extends LockModel, ?> controller,
                        final IOPool<?> pool) {
        super(controller);
        if (null == (this.pool = pool))
            throw new NullPointerException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        /** This class requires ON-DEMAND LOOKUP of its in! */
        final class Input extends DelegatingInputSocket<Entry> {
            @Override
            protected InputSocket<?> getSocket() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(FsAccessOption.CACHE))
                        return controller.getInputSocket(name, options);
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
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        /** This class requires ON-DEMAND LOOKUP of its in! */
        final class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<?> getSocket() {
                assert isWriteLockedByCurrentThread();
                EntryCache cache = caches.get(name);
                if (null == cache) {
                    if (!options.get(FsAccessOption.CACHE))
                        return controller.getOutputSocket(name, options, template);
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
                        final BitField<FsAccessOption> options,
                        final @CheckForNull Entry template)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        controller.mknod(name, type, options, template);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsAccessOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        controller.unlink(name, options);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        NeedsSyncException preSyncEx;
        do {
            preSyncEx = null;
            try {
                preSync(options, handler);
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
            controller.sync(options/*.clear(CLEAR_CACHE)*/, handler);
        } while (null != preSyncEx);
    }

    private <X extends IOException> void
    preSync(final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControlFlowIOException, X {
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
                    } catch (final FsControlFlowIOException ex) {
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
                    } catch (final FsControlFlowIOException ex) {
                        assert false;
                        throw ex;
                    } catch (final IOException ex) {
                        handler.warn(new FsSyncWarningException(getModel(), ex));
                    }
                }
            }
        }
    }

    /** A cache for the contents of an individual archive entry. */
    @Immutable
    private final class EntryCache {
        final FsEntryName name;
        final Cache cache;

        EntryCache(final FsEntryName name) {
            this.name = name;
            this.cache = WRITE_BACK.newCache(CacheController.this.pool);
        }

        InputSocket<?> getInputSocket(BitField<FsAccessOption> options) {
            return cache.configure(new Input(options)).getInputSocket();
        }

        OutputSocket<?> getOutputSocket(BitField<FsAccessOption> options,
                                        @CheckForNull Entry template) {
            return new Output(options, template);
        }

        void flush() throws IOException {
            cache.flush();
        }

        void clear() throws IOException {
            cache.clear();
        }

        void register() {
            assert isWriteLockedByCurrentThread();
            caches.put(name, this);
        }

        /**
         * This class requires LAZY INITIALIZATION of its channel and
         * automatic decoupling on exceptions!
         */
        @NotThreadSafe
        final class Input extends ClutchInputSocket<Entry> {
            final BitField<FsAccessOption> options;

            Input(final BitField<FsAccessOption> options) {
                this.options = options.clear(FsAccessOption.CACHE); // consume
            }

            @Override
            protected InputSocket<? extends Entry> getLazyDelegate() {
                return controller.getInputSocket(name, options);
            }

            @Override
            public InputStream stream() throws IOException {
                return new Stream();
            }

            final class Stream extends DecoratingInputStream {
                @CreatesObligation
                @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                Stream() throws IOException {
                    super(Input.super.stream());
                    assert getModel().isTouched();
                }

                @Override
                public void close() throws IOException {
                    in.close();
                    register();
                }
            } // Stream

            @Override
            public SeekableByteChannel channel(){
                throw new AssertionError();
            }
        } // Input

        /**
         * This class requires LAZY INITIALIZATION of its channel, but NO
         * automatic decoupling on exceptions!
         */
        @NotThreadSafe
        final class Output extends ClutchOutputSocket<Entry> {
            final BitField<FsAccessOption> options;
            final @CheckForNull Entry template;

            Output( final BitField<FsAccessOption> options,
                    final @CheckForNull Entry template) {
                this.options = options.clear(FsAccessOption.CACHE); // consume
                this.template = template;
            }

            @Override
            protected OutputSocket<? extends Entry> getLazyDelegate() {
                return cache.configure( controller.getOutputSocket(
                                            name,
                                            options.clear(EXCLUSIVE),
                                            template))
                            .getOutputSocket();
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                // Note that the super class implementation MUST get
                // bypassed because the channel MUST get kept even upon an
                // exception!
                return getBoundSocket().getLocalTarget();
            }

            @Override
            public OutputStream stream() throws IOException {
                preOutput();
                return new Stream();
            }

            final class Stream extends DecoratingOutputStream {
                @CreatesObligation
                @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                Stream() throws IOException {
                    // Note that the super class implementation MUST get
                    // bypassed because the channel MUST get kept even upon an
                    // exception!
                    //super(Output.super.stream());
                    super(getBoundSocket().stream());
                    register();
                }

                @Override
                public void close() throws IOException {
                    out.close();
                    postOutput();
                }
            } // Stream

            @Override
            public SeekableByteChannel channel() throws IOException {
                preOutput();
                return new Channel();
            }

            final class Channel extends DecoratingSeekableChannel {
                @CreatesObligation
                @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
                Channel() throws IOException {
                    // Note that the super class implementation MUST get
                    // bypassed because the channel MUST get kept even upon an
                    // exception!
                    //super(Nio2Output.super.channel());
                    super(getBoundSocket().channel());
                    register();
                }

                @Override
                public void close() throws IOException {
                    channel.close();
                    postOutput();
                }
            } // Channel

            void preOutput() throws IOException {
                mknod(options, template);
            }

            void postOutput() throws IOException {
                mknod(  options.clear(EXCLUSIVE),
                        null != template ? template : cache.getEntry());
                register();
            }

            void mknod( final BitField<FsAccessOption> options,
                        final @CheckForNull Entry template)
            throws IOException {
                while (true) {
                    try {
                        controller.mknod(name, FILE, options, template);
                        break;
                    } catch (final NeedsSyncException mknodEx) {
                        // In this context, this exception means that the entry
                        // has already been written to the output archive for
                        // the target archive file.

                        // Even if we were asked to create the entry
                        // EXCLUSIVEly, first we must try to get the cache in
                        // sync() with the virtual file system again and retry
                        // the mknod().
                        try {
                            controller.sync(mknodEx);
                            continue; // sync() succeeded, now repeat mknod()
                        } catch (final FsSyncException syncEx) {
                            // sync() failed, maybe just because the current
                            // thread has already acquired some open I/O
                            // resources for the same target archive file, e.g.
                            // an input stream for a copy operation and this
                            // is an artifact of an attempt to acquire the
                            // output stream for a child file system.
                            if (!(syncEx.getCause() instanceof FsResourceOpenException)) {
                                // Too bad, sync() failed because of more
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

                            if (options.get(EXCLUSIVE)) {
                                // We've been asked not to tolerate the
                                // original event but we can't just rethrow the
                                // mknod exception, so let's rethrow the sync
                                // exception instead.
                                throw syncEx;
                            }

                            // Finally, the mknod failed because the entry
                            // has already been output to the target archive
                            // file - so what?!
                            // This should mark only a volatile issue because
                            // the next sync() will sort it out once all the
                            // I/O resources have been closed.
                            // Let's log the sync exception - mind that it has
                            // the mknod exception as its predecessor - and
                            // continue anyway...
                            logger.log(Level.FINE, "ignoring", syncEx);
                            break;
                        }
                    }
                }
            }
        } // Output
    } // EntryCache
}
