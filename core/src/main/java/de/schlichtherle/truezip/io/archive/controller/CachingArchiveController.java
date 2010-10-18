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
 * <li>It increases the performance of concurrent or subsequent read operations.
 * <li>It increases the performance of subsequent write-then-read operations.
 * <li>It decouples the target archive file from read and write operations
 *     so that it can get {@link #sync synced} concurrently.
 * </ul>
 * <p>
 * Caching is automatically activated once an
 * {@link #getInputSocket input socket} with {@link InputOption#CACHE} or an
 * {@link #getOutputSocket output socket} with {@link InputOption#CACHE}
 * is acquired. Subsequent read/write operations will then use the cache
 * regardless if these options where set when the respective socket was
 * acquired or not.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class CachingArchiveController<AE extends ArchiveEntry>
extends FilterArchiveController<AE> {

    private Map<String, PathCache> caches;

    CachingArchiveController(ArchiveController<? extends AE> controller) {
        super(controller);
    }

    private Cache<AE> getCache(
            final String path,
            final boolean create,
            final BitField<InputOption > inputOptions,
            final BitField<OutputOption> outputOptions)
    throws IOException {
        if (create) {
            if (null == caches)
                caches = new HashMap<String, PathCache>();
            return new PathCache(path, inputOptions, outputOptions);
        } else {
            if (null == caches)
                return null;
            return caches.get(path);
        }
    }

    @Override
    public synchronized InputSocket<? extends AE> getInputSocket(
            final String path,
            final BitField<InputOption> options)
    throws IOException {
        if (!options.get(InputOption.CACHE)) {
            final Cache<AE> cache = getCache(path, false, options, null);
            if (null != cache) {
                assert false;
                try {
                    cache.flush();
                } finally {
                    final Cache<AE> cache2 = caches.remove(path);
                    assert cache2 == cache;
                    cache.clear();
                }
            }
            return getController().getInputSocket(path, options);
        }
        return getCache(path, true, options, null).getInputSocket();
    }

    @Override
    public synchronized OutputSocket<? extends AE> getOutputSocket(
            final String path,
            final BitField<OutputOption> options,
            final CommonEntry template)
    throws IOException {
        if (!options.get(OutputOption.CACHE) || options.get(OutputOption.APPEND) || null != template) {
            final Cache<AE> cache = getCache(path, false, null, options);
            if (null != cache) {
                assert false;
                try {
                    cache.flush();
                } finally {
                    final Cache<AE> cache2 = caches.remove(path);
                    assert cache2 == cache;
                    cache.clear();
                }
            }
            return getController().getOutputSocket(path, options, template);
        }

        class Output extends FilterOutputSocket<AE> {
            Output(OutputSocket<? extends AE> output) {
                super(output);
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                getController().mknod(path, FILE, options, null);
                return super.newOutputStream();
            }
        } // class Output

        return new Output(getCache(path, true, null, options).getOutputSocket());
    }

    @Override
    public synchronized <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E, ArchiveControllerException {
        if (null != caches) {
            final boolean abort = options.get(SyncOption.ABORT_CHANGES);
            for (final PathCache cache : caches.values()) {
                try {
                    try {
                        if (!abort)
                            cache.flush();
                    } finally {
                        cache.clear();
                    }
                } catch (IOException ex) {
                    throw builder.fail(new SyncException(this, ex));
                }
            }
            caches.clear();
        }
        super.sync(builder, options);
    }

    private final class PathCache implements Cache<AE> {
        final String path;
        final BitField<InputOption > inputOptions;
        final BitField<OutputOption> outputOptions;
        final Cache<AE> cache;

        PathCache(  final String path,
                    final BitField<InputOption > inputOptions,
                    final BitField<OutputOption> outputOptions)
        throws IOException {
            this.path = path;
            this.inputOptions = null != inputOptions
                    ? inputOptions.clear(InputOption.CACHE)
                    : BitField.noneOf(InputOption.class);
            this.outputOptions = null != outputOptions
                    ? outputOptions.clear(OutputOption.CACHE)
                    : BitField.noneOf(OutputOption.class);
            this.cache = Caches.newInstance(
                    null == inputOptions ? null : new Input(),
                    null == outputOptions ? null : new Output());
        }

        public InputSocket<AE> getInputSocket() throws IOException {
            return cache.getInputSocket();
        }

        public OutputSocket<AE> getOutputSocket() throws IOException {
            return cache.getOutputSocket();
        }

        public void flush() throws IOException {
            cache.flush();
        }

        public void clear() throws IOException {
            cache.clear();
            /*synchronized (CachingArchiveController.this) {
                caches.remove(path);
                cache.clear();
            }*/
        }

        class Input extends FilterInputSocket<AE> {
            Input() throws IOException {
                super(getController().getInputSocket(path, inputOptions));
            }

            @Override
            public InputStream newInputStream() throws IOException {
                synchronized (CachingArchiveController.this) {
                    final InputStream in = super.newInputStream();
                    //caches.put(path, PathCache.this);
                    return in;
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                synchronized (CachingArchiveController.this) {
                    final ReadOnlyFile rof = super.newReadOnlyFile();
                    //caches.put(path, PathCache.this);
                    return rof;
                }
            }
        } // class Input

        class Output extends FilterOutputSocket<AE> {
            Output() throws IOException {
                super(getController().getOutputSocket(path, outputOptions, null));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                synchronized (CachingArchiveController.this) {
                    final OutputStream out = super.newOutputStream();
                    //caches.put(path, PathCache.this);
                    return out;
                }
            }
        } // class Output
    }
}
