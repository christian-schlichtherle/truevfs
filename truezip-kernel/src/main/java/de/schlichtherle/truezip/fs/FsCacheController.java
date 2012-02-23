/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
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
import javax.swing.Icon;

/**
 * A content caching file system controller implements a combined caching and
 * buffering strategy for entry data. Decorating a concurrent file system
 * controller with this class has the following effects:
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
 * @author  Christian Schlichtherle
 * @version $Id$
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
        return new Input(
                delegate.getInputSocket(name, options),
                name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsOutputOption> options,
            final Entry template) {
        return new Output(
                delegate.getOutputSocket(name, options, template),
                name, options, template);
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final Entry template)
    throws IOException {
        assert getModel().isWriteLockedByCurrentThread();
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
        assert getModel().isWriteLockedByCurrentThread();
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
        beforeSync(options, handler);
        // TODO: Consume FsSyncOption.CLEAR_CACHE and clear a flag in the model
        // instead.
        delegate.sync(options/*.clear(CLEAR_CACHE)*/, handler);
    }

    private <X extends IOException> void
    beforeSync( final BitField<FsSyncOption> options,
                final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        assert getModel().isWriteLockedByCurrentThread();
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

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        return delegate.getClosedIcon();
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        return delegate.getEntry(name);
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        return delegate.getOpenIcon();
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        return delegate.isExecutable(name);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return delegate.isReadOnly();
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return delegate.isWritable(name);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times, BitField<FsOutputOption> options) throws IOException {
        return delegate.setTime(name, times, options);
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value, BitField<FsOutputOption> options) throws IOException {
        return delegate.setTime(name, types, value, options);
    }

    private final class Input
    extends DecoratingInputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsInputOption> options;

        Input(  final InputSocket<?> input,
                final FsEntryName name,
                final BitField<FsInputOption> options) {
            super(input);
            this.name = name;
            this.options = options;
        }

        @Override
        protected InputSocket<?> getBoundSocket() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            EntryController controller = controllers.get(name);
            if (null == controller) {
                if (!options.get(FsInputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                //checkWriteLockedByCurrentThread();
                controller = new EntryController(name);
            }
            return controller.configure(options).getInputSocket().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return getBoundSocket().newInputStream();
        }
    } // Input

    private final class Output
    extends DecoratingOutputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsOutputOption> options;
        final @CheckForNull Entry template;

        Output( final OutputSocket<?> output,
                final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(output);
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        protected OutputSocket<?> getBoundSocket() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            EntryController controller = controllers.get(name);
            if (null == controller) {
                if (!options.get(FsOutputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                controller = new EntryController(name);
            }
            return controller.configure(options, template).getOutputSocket().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return getBoundSocket().newOutputStream();
        }
    } // Output

    @Immutable
    private enum SocketFactory {
        OIO() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryController controller,
                    OutputSocket <?> output) {
                return controller.new EntryOutput(output);
            }
        },

        NIO2() {
            @Override
            OutputSocket<?> newOutputSocket(
                    EntryController controller,
                    OutputSocket <?> output) {
                return controller.new Nio2EntryOutput(output);
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
            this.outputOptions = options = options.clear(FsOutputOption.CACHE); // consume
            cache.configure(delegate.getOutputSocket(
                    name,
                    options.clear(EXCLUSIVE),
                    this.template = template));
            output = null;
            return this;
        }

        void flush() throws IOException {
            cache.flush();
        }

        void clear() throws IOException {
            cache.clear();
        }

        InputSocket<?> getInputSocket() {
            final InputSocket<?> input = this.input;
            return null != input ? input : (this.input = cache.getInputSocket());
        }

        OutputSocket<?> getOutputSocket() {
            final OutputSocket<?> output = this.output;
            return null != output
                    ? output
                    : (this.output = SOCKET_FACTORY
                        .newOutputSocket(this, cache.getOutputSocket()));
        }

        void beginOutput() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            delegate.mknod(name, FILE, outputOptions, template);
            assert isTouched();
        }

        void endOutput() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            //assert isTouched(); // may have been concurrently synced!
            delegate.mknod(
                    name,
                    FILE,
                    outputOptions.clear(EXCLUSIVE),
                    null != template ? template : cache.getEntry());
        }

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
                assert getModel().isWriteLockedByCurrentThread();
                final InputStream in = getBoundSocket().newInputStream();
                assert isTouched();
                return new EntryInputStream(in);
            }
        } // EntryInput

        /** An input stream proxy. */
        final class EntryInputStream extends DecoratingInputStream {
            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            EntryInputStream(@WillCloseWhenClosed InputStream in) {
                super(in);
            }

            @Override
            public void close() throws IOException {
                assert getModel().isWriteLockedByCurrentThread();
                delegate.close();
                controllers.put(name, EntryController.this);
            }
        } // EntryInputStream

        /** An output socket proxy which supports NIO.2. */
        final class Nio2EntryOutput extends EntryOutput {
            Nio2EntryOutput(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                beginOutput();
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
                beginOutput();
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
                assert getModel().isWriteLockedByCurrentThread();
                try {
                    delegate.close();
                } finally {
                    endOutput();
                }
            }
        } // EntrySeekableByteChannel

        /** An output stream proxy. */
        final class EntryOutputStream extends DecoratingOutputStream {
            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            EntryOutputStream(@WillCloseWhenClosed OutputStream out) {
                super(out);
            }

            @Override
            public void close() throws IOException {
                assert getModel().isWriteLockedByCurrentThread();
                try {
                    delegate.close();
                } finally {
                    endOutput();
                }
            }
        } // EntryOutputStream
    } // EntryCache
}
