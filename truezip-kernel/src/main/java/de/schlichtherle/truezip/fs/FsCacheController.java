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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
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

    private static final SocketFactory SOCKET_FACTORY = JSE7.AVAILABLE
            ? SocketFactory.NIO2
            : SocketFactory.OIO;

    private final IOPool<?> pool;

    // TODO: Consider using a ConcurrentMap to support concurrent access just
    // protected by a read lock.
    private final Map<FsEntryName, EntryController>
            controllers = new HashMap<FsEntryName, EntryController>();

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
            final InputSocket<?> delegate = FsCacheController.this.delegate
                    .getInputSocket(name, options);

            @Override
            protected InputSocket<?> getDelegate() {
                assert isWriteLockedByCurrentThread();
                EntryController controller = controllers.get(name);
                if (null == controller) {
                    if (!options.get(FsInputOption.CACHE))
                        return delegate;
                    //checkWriteLockedByCurrentThread();
                    controller = new EntryController(name);
                }
                return controller.configure(options).getInputSocket();
            }
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive!
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        class Output extends DelegatingOutputSocket<Entry> {
            final OutputSocket<?> delegate = FsCacheController.this.delegate
                    .getOutputSocket(name, options, template);

            @Override
            protected OutputSocket<?> getDelegate() {
                assert isWriteLockedByCurrentThread();
                EntryController controller = controllers.get(name);
                if (null == controller) {
                    if (!options.get(FsOutputOption.CACHE))
                        return delegate;
                    controller = new EntryController(name);
                }
                return controller.configure(options, template).getOutputSocket();
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final Entry template)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        final EntryController controller = controllers.get(name);
        delegate.mknod(name, type, options, template);
        if (null != controller) {
            controllers.remove(name);
            controller.clear();
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();
        final EntryController controller = controllers.get(name);
        delegate.unlink(name, options);
        if (null != controller) {
            controllers.remove(name);
            controller.clear();
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
        //postSync(options, handler);
    }

    private <X extends IOException> void
    preSync(final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        assert isWriteLockedByCurrentThread();
        if (0 >= controllers.size())
            return;
        final boolean flush = !options.get(ABORT_CHANGES);
        final boolean clear = !flush || options.get(CLEAR_CACHE);
        assert flush || clear;
        final Iterator<EntryController> i = controllers.values().iterator();
        while (i.hasNext()) {
            final EntryController controller = i.next();
            try {
                if (flush) {
                    try {
                        controller.flush();
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
                        controller.clear();
                    } catch (FsControllerException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        handler.warn(new FsSyncWarningException(getModel(), ex));
                    }
                }
            }
        }
    }

    /*private <X extends IOException> void
    postSync(   final BitField<FsSyncOption> options,
                final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        assert isWriteLockedByCurrentThread();
        assert 0 >= controllers.size()
                || !options.get(ABORT_CHANGES) && !options.get(CLEAR_CACHE);
        for (final EntryController controller : controllers.values()) {
            try {
                controller.postSync();
            } catch (FsControllerException ex) {
                throw ex;
            } catch (IOException ex) {
                handler.warn(new FsSyncWarningException(getModel(), ex));
            }
        }
    }*/

    @Immutable
    private enum SocketFactory {
        NIO2() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryController controller,
                    OutputSocket <?> output) {
                return controller.new Nio2EntryOutput(output);
            }
        },
        
        OIO() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryController controller,
                    OutputSocket <?> output) {
                return controller.new EntryOutput(output);
            }
        };

        abstract OutputSocket<?> newOutputSocket(
                EntryController controller,
                OutputSocket <?> output);
    } // SocketFactory

    /** A cache for the contents of an individual file system entry. */
    private final class EntryController {
        final FsEntryName name;
        final IOCache cache;
        @CheckForNull InputSocket<?> input;
        @CheckForNull OutputSocket<?> output;
        @Nullable BitField<FsOutputOption> outputOptions;
        @CheckForNull Entry template;

        EntryController(final FsEntryName name) {
            this.name = name;
            this.cache = WRITE_BACK.newCache(pool);
        }

        EntryController configure(BitField<FsInputOption> options) {
            // Consume FsInputOption.CACHE.
            options = options.clear(FsInputOption.CACHE);
            cache.configure(new EntryInput(delegate.getInputSocket(name, options)));
            input = null;
            return this;
        }

        EntryController configure(  BitField<FsOutputOption> options,
                                    final @CheckForNull Entry template) {
            // Consume FsOutputOption.CACHE.
            this.outputOptions = options = options.clear(FsOutputOption.CACHE);
            cache.configure(delegate.getOutputSocket(
                    name,
                    options.clear(EXCLUSIVE),
                    this.template = template));
            output = null;
            return this;
        }

        void flush() throws IOException {
            try {
                cache.flush();
            } catch (FsNeedsSyncException alreadyFlushed) {
                // FIXME: Explain this!
            }
        }

        void clear() throws IOException {
            cache.clear();
        }

        InputSocket<?> getInputSocket() {
            final InputSocket<?> is = input;
            return null != is ? is : (input = cache.getInputSocket());
        }

        OutputSocket<?> getOutputSocket() {
            final OutputSocket<?> os = output;
            return null != os
                    ? os
                    : (output = SOCKET_FACTORY
                        .newOutputSocket(this, cache.getOutputSocket()));
        }

        void preOutput() throws IOException {
            assert isWriteLockedByCurrentThread();
            delegate.mknod(name, FILE, outputOptions, template);
        }

        void postOutput() throws IOException {
            assert isWriteLockedByCurrentThread();
            delegate.mknod(name, FILE, outputOptions.clear(EXCLUSIVE),
                    null != template ? template : cache.getEntry());
            //output = null;
        }

        /*void postSync() throws IOException {
            assert isWriteLockedByCurrentThread();
            if (null != output)
                delegate.mknod(
                        name,
                        FILE,
                        outputOptions.clear(EXCLUSIVE),
                        template);
        }*/

        /** An input socket proxy. */
        final class EntryInput extends DecoratingInputSocket<Entry> {
            EntryInput(InputSocket <?> input) {
                super(input);
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
                final InputStream in = getBoundSocket().newInputStream();
                assert isTouched();
                return new EntryInputStream(in);
            }
        } // EntryInput

        /** An output socket proxy which supports NIO.2. */
        final class Nio2EntryOutput extends EntryOutput {
            Nio2EntryOutput(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                preOutput();
                final SeekableByteChannel sbc = getBoundSocket().newSeekableByteChannel();
                controllers.put(name, EntryController.this);
                return new EntrySeekableByteChannel(sbc);
            }
        } // Nio2EntryOutput

        /** An output socket proxy. */
        class EntryOutput extends DecoratingOutputSocket<Entry> {
            EntryOutput(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public final OutputStream newOutputStream() throws IOException {
                preOutput();
                final OutputStream out = getBoundSocket().newOutputStream();
                controllers.put(name, EntryController.this);
                return new EntryOutputStream(out);
            }
        } // EntryOutput

        /** A seekable byte channel proxy. */
        final class EntrySeekableByteChannel
        extends DecoratingSeekableByteChannel {
            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            EntrySeekableByteChannel(@WillCloseWhenClosed SeekableByteChannel sbc) {
                super(sbc);
            }

            @Override
            public void close() throws IOException {
                assert isWriteLockedByCurrentThread();
                delegate.close();
                postOutput();
            }
        } // EntrySeekableByteChannel

        /** An input stream proxy. */
        final class EntryInputStream extends DecoratingInputStream {
            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            EntryInputStream(@WillCloseWhenClosed InputStream in) {
                super(in);
            }

            @Override
            public void close() throws IOException {
                assert isWriteLockedByCurrentThread();
                delegate.close();
                controllers.put(name, EntryController.this);
            }
        } // EntryInputStream

        /** An output stream proxy. */
        final class EntryOutputStream extends DecoratingOutputStream {
            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            EntryOutputStream(@WillCloseWhenClosed OutputStream out) {
                super(out);
            }

            @Override
            public void close() throws IOException {
                assert isWriteLockedByCurrentThread();
                delegate.close();
                postOutput();
            }
        } // EntryOutputStream
    } // EntryCache
}
