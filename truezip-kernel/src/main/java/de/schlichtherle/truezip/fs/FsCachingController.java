/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs;

import net.jcip.annotations.Immutable;
import de.schlichtherle.truezip.util.JSE7;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.InputStream;
import de.schlichtherle.truezip.socket.IOCache.Strategy;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.IOCache;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.socket.IOCache.Strategy.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;

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
extends FsDecoratingController< FsConcurrentModel,
                                FsController<? extends FsConcurrentModel>> {

    private static final Strategy STRATEGY = WRITE_BACK;

    private final IOPool<?> pool;
    private final Map<FsEntryName, EntryCache>
            caches = new HashMap<FsEntryName, EntryCache>();

    /**
     * Constructs a new content caching file system controller.
     *
     * @param controller the decorated file system controller.
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
        return new CacheInputSocket(name, options);
    }

    private final class CacheInputSocket extends DecoratingInputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsInputOption> options;

        CacheInputSocket(final FsEntryName name, final BitField<FsInputOption> options) {
            super(delegate.getInputSocket(name, options));
            this.name = name;
            this.options = options;
        }

        @Override
        public InputSocket<?> getBoundSocket() throws IOException {
            EntryCache cache = caches.get(name);
            if (null == cache) {
                if (!options.get(FsInputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                getModel().assertWriteLockedByCurrentThread();
                cache = new EntryCache(name);
            }
            return cache.configure(options).getInputSocket().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().newInputStream();
        }
    } // Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            Entry template) {
        return new CacheOutputSocket(name, options, template);
    }

    private final class CacheOutputSocket extends DecoratingOutputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsOutputOption> options;
        final @CheckForNull Entry template;

        CacheOutputSocket( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        public OutputSocket<?> getBoundSocket() throws IOException {
            EntryCache cache = caches.get(name);
            if (null == cache) {
                if (!options.get(FsOutputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                cache = new EntryCache(name);
            } else {
                if (options.get(APPEND)) {
                    // This combination of features would be expected to work
                    // with a WRITE_THROUGH cache strategy.
                    // However, we are using WRITE_BACK for performance reasons
                    // and we can't change the strategy because the cache might
                    // be busy on input!
                    // So if this is really required, change the caching
                    // strategy to WRITE_THROUGH and bear the performance
                    // impact.
                    assert WRITE_THROUGH == STRATEGY; // TODO: Check and fix this!
                    cache.flush();
                }
            }
            return cache.configure(options, template).getOutputSocket().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            // This is the same as the code in the super class, so overriding
            // is redundant, but it makes the stack trace much easier to digest.
            return getBoundSocket().newOutputStream();
        }
    } // Output

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final Entry template)
    throws IOException {
        assert getModel().isWriteLockedByCurrentThread();

        final EntryCache cache = caches.get(name);
        if (null != cache) {
            //cache.flush(); // redundant
            delegate.mknod(name, type, options, template);
            caches.remove(name);
            cache.clear();
        } else {
            delegate.mknod(name, type, options, template);
        }
    }

    @Override
    public void unlink(final FsEntryName name)
    throws IOException {
        assert getModel().isWriteLockedByCurrentThread();

        final EntryCache cache = caches.get(name);
        if (null != cache) {
            //cache.flush(); // redundant
            delegate.unlink(name);
            caches.remove(name);
            cache.clear();
        } else {
            delegate.unlink(name);
        }
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        beforeSync(options, handler);
        delegate.sync(options.clear(CLEAR_CACHE), handler);
    }

    private <X extends IOException> void beforeSync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        assert getModel().isWriteLockedByCurrentThread();
        if (0 >= caches.size())
            return;
        final boolean flush = !options.get(ABORT_CHANGES);
        final boolean clear = !flush || options.get(CLEAR_CACHE);
        for (final Iterator<EntryCache> i = caches.values().iterator(); i.hasNext(); ) {
            final EntryCache cache = i.next();
            try {
                if (flush)
                    cache.flush();
            } catch (IOException ex) {
                throw handler.fail(new FsSyncException(getModel(), ex));
            } finally  {
                try {
                    if (clear) {
                        i.remove();
                        cache.clear();
                    }
                } catch (IOException ex) {
                    handler.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        }
    }

    private static final EntryOutputSocketFactory FACTORY = JSE7.AVAILABLE
            ? EntryOutputSocketFactory.NIO
            : EntryOutputSocketFactory.OIO;

    @Immutable
    private enum EntryOutputSocketFactory {
        OIO() {
            @Override
            OutputSocket<?> newOutputSocket(EntryCache cache, OutputSocket <?> output) {
                return cache.new EntryOutputSocket(output);
            }
        },

        NIO() {
            @Override
            OutputSocket<?> newOutputSocket(EntryCache cache, OutputSocket <?> output) {
                return cache.new Nio2EntryOutputSocket(output);
            }
        };
        
        abstract OutputSocket<?> newOutputSocket(EntryCache cache, OutputSocket <?> output);
    } // EntryOutputSocketFactory

    /** A cache for the contents of an individual file system entry. */
    private class EntryCache {
        private final FsEntryName name;
        private final IOCache cache;
        private volatile @CheckForNull InputSocket<?> input;
        private volatile @CheckForNull OutputSocket<?> output;
        private volatile @Nullable BitField<FsOutputOption> mknodOptions;
        private volatile @CheckForNull Entry template;

        EntryCache(final FsEntryName name) {
            this.name = name;
            this.cache = STRATEGY.newCache(pool);
        }

        EntryCache configure(BitField<FsInputOption> options) {
            options = options.clear(FsInputOption.CACHE); // consume
            cache.configure(new EntryInputSocket(
                    delegate.getInputSocket(name, options)));
            input = null;
            return this;
        }

        EntryCache configure(   BitField<FsOutputOption> options,
                                final @CheckForNull Entry template) {
            // Consume FsOutputOption.CACHE.
            this.mknodOptions = options = options.clear(FsOutputOption.CACHE); // consume
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
                    : (this.output = FACTORY.newOutputSocket(
                        this,
                        cache.getOutputSocket()));
        }

        private final class EntryInputSocket
        extends DecoratingInputSocket<Entry> {
            EntryInputSocket(InputSocket <?> input) {
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
                assert getModel().isTouched();
                caches.put(name, EntryCache.this);
                return in;
            }
        } // EntryInputSocket

        /** An output socket proxy which supports NIO.2. */
        private final class Nio2EntryOutputSocket
        extends EntryOutputSocket {
            Nio2EntryOutputSocket(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                assert getModel().isWriteLockedByCurrentThread();
                delegate.mknod(name, FILE, mknodOptions, template);
                assert getModel().isTouched();
                final SeekableByteChannel sbc = getBoundSocket().newSeekableByteChannel();
                caches.put(name, EntryCache.this);
                return new EntrySeekableByteChannel(sbc);
            }
        } // Nio2EntryOutputSocket

        /** An output socket proxy. */
        private class EntryOutputSocket
        extends DecoratingOutputSocket<Entry> {
            EntryOutputSocket(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public final OutputStream newOutputStream() throws IOException {
                assert getModel().isWriteLockedByCurrentThread();
                delegate.mknod(name, FILE, mknodOptions, template);
                assert getModel().isTouched();
                final OutputStream out = getBoundSocket().newOutputStream();
                caches.put(name, EntryCache.this);
                return new EntryOutputStream(out);
            }
        } // EntryOutputSocket

        /** An output stream proxy. */
        private final class EntrySeekableByteChannel
        extends DecoratingSeekableByteChannel {
            EntrySeekableByteChannel(SeekableByteChannel sbc) {
                super(sbc);
            }

            @Override
            public void close() throws IOException {
                try {
                    delegate.close();
                } finally {
                    if (null == template)
                        FsCachingController.this.delegate.mknod(
                                name,
                                FILE,
                                mknodOptions.clear(EXCLUSIVE),
                                cache.getEntry());
                }
            }
        } // EntrySeekableByteChannel

        /** An output stream proxy. */
        private final class EntryOutputStream
        extends DecoratingOutputStream {
            EntryOutputStream(OutputStream out) {
                super(out);
            }

            @Override
            public void close() throws IOException {
                try {
                    delegate.close();
                } finally {
                    if (null == template)
                        FsCachingController.this.delegate.mknod(
                                name,
                                FILE,
                                mknodOptions.clear(EXCLUSIVE),
                                cache.getEntry());
                }
            }
        } // EntryOutputStream
    } // EntryCache
}
