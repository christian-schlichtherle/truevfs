/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.IOCache;
import de.schlichtherle.truezip.socket.IOCache.Strategy;
import static de.schlichtherle.truezip.socket.IOCache.Strategy.*;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;

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
@DefaultAnnotation(NonNull.class)
public final class FsCachingController
extends FsDecoratingConcurrentModelController<
        FsController<? extends FsConcurrentModel>> {

    private static final BitField<FsSyncOption>
            ABORT_CHANGES_OPTIONS = BitField.of(ABORT_CHANGES);

    private static final EntrySocketFactory
            ENTRY_SOCKET_FACTORY = JSE7.AVAILABLE
                ? EntrySocketFactory.NIO2
                : EntrySocketFactory.OIO;

    private static final Strategy STRATEGY = WRITE_BACK;

    private final IOPool<?> pool;
    private final Map<FsEntryName, EntryController>
            controllers = new HashMap<FsEntryName, EntryController>();

    /**
     * Constructs a new content caching file system controller.
     *
     * @param controller the decorated concurrent file system controller.
     * @param pool the pool of temporary entries to hold the copied entry data.
     */
    public FsCachingController(
            final FsController<? extends FsConcurrentModel> controller,
            final IOPool<?> pool) {
        super(controller);
        if (null == pool)
            throw new NullPointerException();
        this.pool = pool;
    }

    @Override
    public InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsInputOption> options) {
        return new Input(
                delegate.getInputSocket(name, options),
                name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            Entry template) {
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
        assert isWriteLockedByCurrentThread();

        final EntryController controller = controllers.get(name);
        if (null != controller) {
            //cache.flush(); // redundant
            delegate.mknod(name, type, options, template);
            controllers.remove(name);
            controller.clear();
        } else {
            delegate.mknod(name, type, options, template);
        }
    }

    @Override
    public void unlink(final FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        assert isWriteLockedByCurrentThread();

        if (name.isRoot()) {
            try {
                unlink0(name, options);
            } catch (FsFalsePositiveException ex) {
                try {
                    // Next, the FsFederatingController will try to unlink the
                    // target archive file in the parent file system, so we need
                    // to reset anyway.
                    // The only effect of calling sync for a false positive
                    // archive file is that it will reset the mount state so
                    // that the file system can be successfully mounted again
                    // if the target archive file is subsequently modified to
                    // be a regular archive file.
                    sync(ABORT_CHANGES_OPTIONS);
                } catch (IOException cannotHappen) {
                    throw new AssertionError(cannotHappen);
                }
                // Continue with unlinking the target archive file in the parent
                // file system.
                throw ex;
            }
            sync(ABORT_CHANGES_OPTIONS);
        } else {
            unlink0(name, options);
        }
    }

    private void unlink0(final FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        final EntryController controller = controllers.get(name);
        if (null != controller) {
            //cache.flush(); // redundant
            delegate.unlink(name, options);
            controllers.remove(name);
            controller.clear();
        } else {
            delegate.unlink(name, options);
        }
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        beforeSync(options, handler);
        // TODO: Consume FsSyncOption.CLEAR_CACHE and clear a flag in the model
        // instead.
        delegate.sync(options/*.clear(CLEAR_CACHE)*/, handler);
    }

    private <X extends IOException> void beforeSync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        assert isWriteLockedByCurrentThread();
        if (0 >= controllers.size())
            return;
        final boolean flush = !options.get(ABORT_CHANGES);
        final boolean clear = !flush || options.get(CLEAR_CACHE);
        final Iterator<EntryController> i = controllers.values().iterator();
        while (i.hasNext()) {
            final EntryController controller = i.next();
            try {
                if (flush)
                    controller.flush();
            } catch (IOException ex) {
                throw handler.fail(new FsSyncException(getModel(), ex));
            } finally  {
                try {
                    if (clear) {
                        i.remove();
                        controller.clear();
                    }
                } catch (IOException ex) {
                    handler.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        }
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
        public InputSocket<?> getBoundSocket() throws IOException {
            EntryController controller = controllers.get(name);
            if (null == controller) {
                if (!options.get(FsInputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                assertWriteLockedByCurrentThread();
                controller = new EntryController(name);
            }
            return controller.configure(options).getInputSocket().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
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
        public OutputSocket<?> getBoundSocket() throws IOException {
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
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newOutputStream();
        }
    } // Output

    @Immutable
    private enum EntrySocketFactory {
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
    } // EntryOutputFactory

    /** A cache for the contents of an individual file system entry. */
    private final class EntryController {
        final FsEntryName name;
        final IOCache cache;
        volatile @CheckForNull InputSocket<?> input;
        volatile @CheckForNull OutputSocket<?> output;
        volatile @Nullable BitField<FsOutputOption> outputOptions;
        volatile @CheckForNull Entry template;

        EntryController(final FsEntryName name) {
            this.name = name;
            this.cache = STRATEGY.newCache(pool);
        }

        EntryController configure(BitField<FsInputOption> options) {
            // Consume FsInputOption.CACHE.
            options = options.clear(FsInputOption.CACHE);
            cache.configure(new EntryInput(delegate.getInputSocket(name, options)));
            input = null;
            return this;
        }

        EntryController configure(   BitField<FsOutputOption> options,
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
                    : (this.output = ENTRY_SOCKET_FACTORY
                        .newOutputSocket(this, cache.getOutputSocket()));
        }

        void beginOutput() throws IOException {
            assert isWriteLockedByCurrentThread();
            delegate.mknod(name, FILE, outputOptions, template);
            assert isTouched();
        }

        void commitOutput() throws IOException {
            assert isWriteLockedByCurrentThread();
            assert isTouched();
            if (null != template)
                return;
            delegate.mknod(
                    name,
                    FILE,
                    outputOptions.clear(EXCLUSIVE),
                    cache.getEntry());
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
                assert isWriteLockedByCurrentThread();
                final InputStream in = getBoundSocket().newInputStream();
                assert isTouched();
                return new EntryInputStream(in);
            }
        } // EntryInput

        /** An input stream proxy. */
        final class EntryInputStream extends DecoratingInputStream {
            EntryInputStream(InputStream in) {
                super(in);
            }

            @Override
            public void close() throws IOException {
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
            EntrySeekableByteChannel(SeekableByteChannel sbc) {
                super(sbc);
            }

            @Override
            public void close() throws IOException {
                assert isWriteLockedByCurrentThread();
                try {
                    delegate.close();
                } finally {
                    commitOutput();
                }
            }
        } // EntrySeekableByteChannel

        /** An output stream proxy. */
        final class EntryOutputStream extends DecoratingOutputStream {
            EntryOutputStream(OutputStream out) {
                super(out);
            }

            @Override
            public void close() throws IOException {
                assert isWriteLockedByCurrentThread();
                try {
                    delegate.close();
                } finally {
                    commitOutput();
                }
            }
        } // EntryOutputStream
    } // EntryCache
}
