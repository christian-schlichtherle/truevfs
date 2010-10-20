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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.InputStream;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.Caches;
import java.util.HashMap;
import de.schlichtherle.truezip.io.socket.Cache;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.util.Map;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.FILE;

/**
 * A caching archive controller implements a caching strategy for entries
 * within its target archive file.
 * Decorating an archive controller with this class has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the archive
 *     entry and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the data from the archive entry again until the
 *     target archive file gets {@link #sync synced}.
 * <li>Any data written to the cache will get written to the target archive
 *     file if and only if the target archive file gets {@link #sync synced}.
 * <li>After a write operation, the data will be stored in the cache for
 *     subsequent read operations until the target archive file gets
 *     {@link #sync synced}.
 * </ul>
 * <p>
 * Caching an archive entry is automatically activated once an
 * {@link #getInputSocket input socket} with {@link InputOption#CACHE} or an
 * {@link #getOutputSocket output socket} with {@link InputOption#CACHE}
 * is acquired. Subsequent read/write operations for the archive entry will
 * then use the cache regardless if these options were set when the respective
 * socket was acquired or not.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
// FIXME: Make this work as advertised!
final class CachingArchiveController<AE extends ArchiveEntry>
extends FilterArchiveController<AE> {

    private Map<String, EntryCache> caches;

    CachingArchiveController(ArchiveController<? extends AE> controller) {
        super(controller);
    }

    private Cache<AE> getCache( final boolean create,
                                final String path,
                                final BitField<InputOption > inputOptions,
                                final BitField<OutputOption> outputOptions) {
        if (create) {
            if (null == caches)
                caches = new HashMap<String, EntryCache>();
            return new EntryCache(path, inputOptions, outputOptions);
        } else {
            if (null == caches)
                return null;
            return caches.get(path);
        }
    }

    @Override
    public synchronized InputSocket<? extends AE> getInputSocket(
            final String path,
            final BitField<InputOption> options) {
        Cache<AE> cache = null;
        if (!options.get(InputOption.CACHE)
                && null == (cache = getCache(false, path, options, null))) {
            return getController().getInputSocket(path, options);
        }
        return (null != cache ? cache : getCache(true, path, options, null))
                .getInputSocket();
    }

    @Override
    public synchronized OutputSocket<? extends AE> getOutputSocket(
            final String path,
            final BitField<OutputOption> options,
            final CommonEntry template) {
        Cache<AE> cache = null;
        if (!options.get(OutputOption.CACHE)
                && null == (cache = getCache(false, path, null, options))
                || options.get(OutputOption.APPEND) || null != template) {

            class DirectOutput extends FilterOutputSocket<AE> {
                final Cache<AE> cache;

                DirectOutput(final Cache<AE> cache) {
                    super(getController().getOutputSocket(path, options, template));
                    this.cache = cache;
                }

                @Override
                public OutputStream newOutputStream() throws IOException {
                    if (null != cache) {
                        try {
                            cache.flush();
                        } finally {
                            final Cache<AE> cache2 = caches.remove(path);
                            assert cache2 == cache;
                            cache.clear();
                        }
                    }
                    return super.newOutputStream();
                }
            } // class DirectOutput

            return new DirectOutput(cache);
        }

        class CachedOutput extends FilterOutputSocket<AE> {
            CachedOutput(final Cache<AE> cache) {
                super((null != cache ? cache : getCache(true, path, null, options))
                .getOutputSocket());
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                getController().mknod(path, FILE, options, null);
                return super.newOutputStream();
            }
        } // class CachedOutput

        return new CachedOutput(cache);
    }

    @Override
    public synchronized <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E, ArchiveControllerException {
        if (null != caches) {
            final boolean abort = options.get(SyncOption.ABORT_CHANGES);
            for (final EntryCache cache : caches.values()) {
                try {
                    try {
                        if (!abort)
                            cache.flush();
                    } finally {
                        cache.clear();
                    }
                } catch (IOException ex) {
                    throw builder.fail(new SyncException(getModel(), ex));
                }
            }
            caches.clear();
        }
        super.sync(builder, options);
    }

    private final class EntryCache implements Cache<AE> {
        final String path;
        final BitField<InputOption > inputOptions;
        final BitField<OutputOption> outputOptions;
        final Cache<AE> cache;

        EntryCache(  final String path,
                    final BitField<InputOption > inputOptions,
                    final BitField<OutputOption> outputOptions) {
            this.path = path;
            this.inputOptions = null != inputOptions
                    ? inputOptions.clear(InputOption.CACHE)
                    : BitField.noneOf(InputOption.class);
            this.outputOptions = null != outputOptions
                    ? outputOptions.clear(OutputOption.CACHE)
                    : BitField.noneOf(OutputOption.class);
            this.cache = Caches.newInstance(new Input(), new Output()); // FIXME: this doesn't work with eager socket implementations!
        }

        @Override
        public InputSocket<AE> getInputSocket() {
            return cache.getInputSocket();
        }

        @Override
        public OutputSocket<AE> getOutputSocket() {
            return cache.getOutputSocket();
        }

        @Override
        public void flush() throws IOException {
            cache.flush();
        }

        @Override
        public void clear() throws IOException {
            cache.clear();
        }

        class Input extends FilterInputSocket<AE> {
            Input() {
                super(getController().getInputSocket(path, inputOptions));
            }

            @Override
            public InputStream newInputStream() throws IOException {
                synchronized (CachingArchiveController.this) {
                    final InputStream in = super.newInputStream();
                    caches.put(path, EntryCache.this);
                    return in;
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                synchronized (CachingArchiveController.this) {
                    final ReadOnlyFile rof = super.newReadOnlyFile();
                    caches.put(path, EntryCache.this);
                    return rof;
                }
            }
        } // class Input

        class Output extends FilterOutputSocket<AE> {
            Output() {
                super(getController().getOutputSocket(path, outputOptions, null));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                synchronized (CachingArchiveController.this) {
                    final OutputStream out = super.newOutputStream();
                    caches.put(path, EntryCache.this);
                    return out;
                }
            }
        } // class Output
    }
}
