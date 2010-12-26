/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem.concurrent;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.DecoratingFileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.InputOption;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.filesystem.SyncWarningException;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.socket.Cache.Strategy.*;
import static de.schlichtherle.truezip.io.filesystem.OutputOption.*;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;

/**
 * A content caching archive controller implements a caching strategy for entry
 * data. Decorating a concurrent file system controller with this class has the
 * following effects:
 * <ul>
 * <li>Upon the first read operation, the entry data will be read from the
 *     backing store and temporarily stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the entry data from the backing store again until
 *     the file system gets {@link #sync synced}.
 * <li>At the discretion of the internal caching strategy, entry data written
 *     to the cache may not be written to the backing store until the file
 *     system gets {@link #sync synced}.
 * <li>After a write operation, the entry data will be stored in the cache
 *     for subsequent read operations until the file system gets
 *     {@link #sync synced}.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the cached
 *     entry data.
 * </ul>
 * Note that caching file system entry data is performed only if an
 * {@link #getInputSocket input socket} with the input option
 * {@link InputOption#CACHE} is used or an
 * {@link #getOutputSocket output socket} with the output option
 * {@link OutputOption#CACHE} is used <em>and</em> this socket is <em>not</em>
 * connected.
 *
 * @param   <M> The type of the file system model.
 * @param   <C> The type of the decorated file system controller.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class ContentCachingFileSystemController<
        M extends ConcurrentFileSystemModel,
        C extends FileSystemController<? extends M>>
extends DecoratingFileSystemController<M, C>
/*implements FileSystemSyncListener*/ {

    private final IOPool<?> pool;
    private final Map<FileSystemEntryName, Cache> caches
            = new HashMap<FileSystemEntryName, Cache>();

    /**
     * Constructs a new content caching file system controller.
     *
     * @param controller the decorated file system controller.
     * @param pool the pool of temporary entries to cache the entry data.
     */
    public ContentCachingFileSystemController(@NonNull final C controller,
                                            @NonNull final IOPool<?> pool) {
        super(controller);
        if (null == pool)
            throw new NullPointerException();
        this.pool = pool;
        //getModel().addFileSystemSyncListener(this);
    }

    @Override
    public InputSocket<?> getInputSocket(
            FileSystemEntryName name,
            BitField<InputOption> options) {
        return new Input(name, options);
    }

    private class Input extends DecoratingInputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<InputOption> options;

        Input(final FileSystemEntryName name, final BitField<InputOption> options) {
            super(delegate.getInputSocket(name, options));
            this.name = name;
            this.options = options;
        }

        @Override
        public InputSocket<?> getBoundSocket() throws IOException {
            Cache cache = caches.get(name);
            if (null == cache) {
                if (!options.get(InputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                cache = new Cache(name);
            }
            return cache.configure(options).getInputSocket().bind(this);
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends DecoratingOutputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<OutputOption> options;
        final Entry template;

        Output( final FileSystemEntryName name,
                final BitField<OutputOption> options,
                final Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        public OutputSocket<?> getBoundSocket() throws IOException {
            assert getModel().writeLock().isHeldByCurrentThread();

            Cache cache = caches.get(name);
            if (null == cache) {
                if (!options.get(OutputOption.CACHE))
                    return super.getBoundSocket(); // don't cache
                cache = new Cache(name);
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
                    assert false; // FIXME: Check and fix this!
                    cache.flush();
                }
            }
            makeMarkerEntry();
            return cache.configure(options, template).getOutputSocket().bind(this);
        }

        /**
         * Ensure the existence of an entry in the file system.
         */
        private void makeMarkerEntry() throws IOException {
            boolean exists = false;
            try {
                exists = null != delegate.getEntry(name);
            } catch (IOException ignored) {
                // This could be a FalsePositiveException, which would cause
                // unwanted resolution to the parent controller.
            }
            if (exists) {
                getModel().setTouched(true);
            } else {
                // EXCLUSIVE is actually redundant, but provided for clarity.
                delegate.mknod(name, FILE, options.set(EXCLUSIVE), template);
            }
            assert getModel().isTouched();
        }
    } // class Output

    @Override
    public void unlink(final FileSystemEntryName name) throws IOException {
        assert getModel().writeLock().isHeldByCurrentThread();

        delegate.unlink(name);
        final Cache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public <X extends IOException> void sync(
            @NonNull final BitField<SyncOption> options,
            @NonNull final ExceptionHandler<? super SyncException, X> handler)
    throws X, FileSystemException {
        beforeSync(options, handler);
        delegate.sync(options.clear(CLEAR_CACHE), handler);
    }

    /*@Override
    public <X extends IOException>
    void beforeSync(final FileSystemSyncEvent<X> event)
    throws X, FileSystemException {
        beforeSync(event.getOptions(), event.getHandler());
    }*/

    private <X extends IOException> void beforeSync(
            @NonNull final BitField<SyncOption> options,
            @NonNull final ExceptionHandler<? super SyncException, X> handler)
    throws X, FileSystemException {
        assert getModel().writeLock().isHeldByCurrentThread();

        if (0 >= caches.size())
            return;

        final boolean flush = !options.get(ABORT_CHANGES);
        final boolean clear = !flush || options.get(CLEAR_CACHE);
        for (final Iterator<Cache> i = caches.values().iterator(); i.hasNext(); ) {
            final Cache cache = i.next();
            try {
                if (flush)
                    cache.flush();
            } catch (IOException ex) {
                throw handler.fail(new SyncException(getModel(), ex));
            } finally  {
                try {
                    if (clear) {
                        i.remove();
                        cache.clear();
                    }
                } catch (IOException ex) {
                    handler.warn(new SyncWarningException(getModel(), ex));
                }
            }
        }
    }

    private static final BitField<InputOption> NO_INPUT_OPTIONS
            = BitField.noneOf(InputOption.class);

    private static final BitField<OutputOption> NO_OUTPUT_OPTIONS
            = BitField.noneOf(OutputOption.class);

    private final class Cache {
        private final FileSystemEntryName name;
        private final de.schlichtherle.truezip.io.socket.Cache<Entry> cache;
        private volatile InputSocket<Entry> input;
        private volatile OutputSocket<Entry> output;
        private volatile BitField<InputOption> inputOptions;
        private volatile BitField<OutputOption> outputOptions;
        private volatile Entry template;

        Cache(@NonNull final FileSystemEntryName name) {
            this.name = name;
            this.cache = WRITE_BACK.newCache(pool); // FIXME: WRITE_THROUGH leaves temps - why!?
            configure(NO_INPUT_OPTIONS);
            configure(NO_OUTPUT_OPTIONS, null);
        }

        @NonNull
        public Cache configure(@NonNull BitField<InputOption> options) {
            cache.configure(new RegisteringInputSocket(delegate.getInputSocket(
                    name,
                    this.inputOptions = options.clear(InputOption.CACHE))));
            input = null;
            return this;
        }

        @NonNull
        public Cache configure( @NonNull BitField<OutputOption> options,
                                @Nullable Entry template) {
            cache.configure(delegate.getOutputSocket(
                    name,
                    this.outputOptions = options.clear(OutputOption.CACHE),
                    this.template = template));
            output = null;
            return this;
        }

        public InputSocket<Entry> getInputSocket() {
            return null != input ? input : (input = cache.getInputSocket());
        }

        public OutputSocket<Entry> getOutputSocket() {
            return null != output ? output : (output
                    = new RegisteringOutputSocket(cache.getOutputSocket()));
        }

        public void flush() throws IOException {
            cache.flush();
        }

        public void clear() throws IOException {
            cache.clear();
        }

        private final class RegisteringInputSocket
        extends DecoratingInputSocket<Entry> {
            private RegisteringInputSocket(final InputSocket <?> input) {
                super(input);
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                getModel().assertWriteLockedByCurrentThread();

                if (null != getBoundSocket().getPeerTarget()) {
                    // The data for connected sockets should not get cached
                    // because... FIXME: Why exactly?!
                    // So we flush and bypass the cache.
                    flush();
                    return delegate .getInputSocket(name, inputOptions)
                                    .newReadOnlyFile();
                }

                final ReadOnlyFile rof = getBoundSocket().newReadOnlyFile();
                caches.put(name, Cache.this);
                return rof;
            }

            @Override
            public InputStream newInputStream() throws IOException {
                getModel().assertWriteLockedByCurrentThread();

                if (null != getBoundSocket().getPeerTarget()) {
                    // Dito.
                    flush();
                    return delegate .getInputSocket(name, inputOptions)
                                    .newInputStream();
                }

                final InputStream in = getBoundSocket().newInputStream();
                caches.put(name, Cache.this);
                return in;
            }
        } // class RegisteringInputSocket

        private final class RegisteringOutputSocket
        extends DecoratingOutputSocket<Entry> {
            private RegisteringOutputSocket(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                assert getModel().writeLock().isHeldByCurrentThread();

                if (null != getBoundSocket().getPeerTarget()) {
                    // Dito, but this time we clear and bypass the cache.
                    clear();
                    return delegate
                            .getOutputSocket(name, outputOptions, template)
                            .newOutputStream();
                }

                final OutputStream out = getBoundSocket().newOutputStream();
                //makeMarkerEntry();
                caches.put(name, Cache.this);
                return out;
            }
        } // class RegisteringOutputSocket
    } // class Cache
}
