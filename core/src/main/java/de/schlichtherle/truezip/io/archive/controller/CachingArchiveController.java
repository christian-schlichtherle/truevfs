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

import de.schlichtherle.truezip.io.filesystem.EntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncWarningException;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.IOCache;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static de.schlichtherle.truezip.io.entry.Entry.Type.FILE;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.CLEAR_CACHE;

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
 * Caching an archive entry is automatically used for an
 * {@link #getInputSocket input socket} with the input option
 * {@link InputOption#CACHE} set or an {@link #getOutputSocket output socket}
 * with the output option {@link OutputOption#CACHE} set.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class CachingArchiveController<E extends ArchiveEntry>
extends FilterArchiveController<E, ArchiveController<? extends E>> {

    private final Map<EntryName, EntryCache> caches
            = new HashMap<EntryName, EntryCache>();

    public CachingArchiveController(ArchiveController<? extends E> controller) {
        super(controller);
    }

    @Override
    public InputSocket<E> getInputSocket(
            final EntryName path,
            final BitField<InputOption> options) {
        return new Input(path, options);
    }

    private class Input extends FilterInputSocket<E> {
        final EntryName path;
        final BitField<InputOption> options;

        Input(final EntryName path, final BitField<InputOption> options) {
            super(controller.getInputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public InputSocket<? extends E> getBoundSocket() throws IOException {
            final IOCache<E> cache = caches.get(path);
            if (null == cache && !options.get(InputOption.CACHE))
                return super.getBoundSocket(); // bypass the cache
            return (null != cache ? cache : new EntryCache(path,
                        options, BitField.noneOf(OutputOption.class)))
                    .getInputSocket()
                    .bind(this);
        }
    } // class Input

    @Override
    public OutputSocket<E> getOutputSocket(
            final EntryName path,
            final BitField<OutputOption> options,
            final Entry template) {
        return new Output(path, options, template);
    }

    private class Output extends FilterOutputSocket<E> {
        final EntryName path;
        final BitField<OutputOption> options;
        final Entry template;

        Output( final EntryName path,
                final BitField<OutputOption> options,
                final Entry template) {
            super(controller.getOutputSocket(path, options, template));
            this.path = path;
            this.options = options;
            this.template = template;
        }

        @Override
        public OutputSocket<? extends E> getBoundSocket() throws IOException {
            assert getModel().writeLock().isHeldByCurrentThread();

            final IOCache<E> cache = caches.get(path);
            if (null == cache && !options.get(OutputOption.CACHE)
                    || options.get(OutputOption.APPEND)
                    || null != template) {
                if (null != cache) {
                    getModel().assertWriteLockedByCurrentThread();
                    try {
                        cache.flush();
                    } finally {
                        final IOCache<E> cache2 = caches.remove(path);
                        assert cache2 == cache;
                        cache.clear();
                    }
                }
                return super.getBoundSocket(); // bypass the cache
            }
            // Create marker entry and mind CREATE_PARENTS!
            controller.mknod(path, FILE, options, null);
            getModel().setTouched(true);
            return (null != cache ? cache : new EntryCache(path,
                        BitField.noneOf(InputOption.class), options))
                    .getOutputSocket()
                    .bind(this);
        }
    } // class Output

    @Override
    public void unlink(final EntryName path) throws IOException {
        assert getModel().writeLock().isHeldByCurrentThread();

        controller.unlink(path);
        final IOCache<E> cache = caches.remove(path);
        if (null != cache)
            cache.clear();
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X, FileSystemException {
        assert getModel().writeLock().isHeldByCurrentThread();

        if (0 < caches.size()) {
            final boolean flush = !options.get(ABORT_CHANGES);
            final boolean clear = !flush || options.get(CLEAR_CACHE);
            for (final EntryCache cache : caches.values()) {
                try {
                    if (flush)
                        cache.flush();
                } catch (IOException ex) {
                    throw builder.fail(new SyncException(getModel(), ex));
                } finally  {
                    try {
                        if (clear)
                            cache.clear();
                    } catch (IOException ex) {
                        builder.warn(new SyncWarningException(getModel(), ex));
                    }
                }
            }
            if (clear)
                caches.clear();
        }
        controller.sync(builder, options.clear(CLEAR_CACHE));
    }

    private final class EntryCache implements IOCache<E> {
        final EntryName path;
        final BitField<InputOption> inputOptions;
        final BitField<OutputOption> outputOptions;
        final IOCache<E> cache;
        final InputSocket <E> input;
        final OutputSocket<E> output;

        EntryCache( final EntryName path,
                    final BitField<InputOption > inputOptions,
                    final BitField<OutputOption> outputOptions) {
            this.path = path;
            this.inputOptions = inputOptions.clear(InputOption.CACHE);
            this.outputOptions = outputOptions.clear(OutputOption.CACHE);
            this.cache = IOCache.Strategy.WRITE_BACK.newCache(
                    new RegisteringInputSocket(
                        controller.getInputSocket(path, this.inputOptions)),
                    controller.getOutputSocket(path, this.outputOptions, null));
            this.input = cache.getInputSocket();
            this.output = new RegisteringOutputSocket(cache.getOutputSocket());
        }

        @Override
        public InputSocket<E> getInputSocket() {
            return input;
        }

        @Override
        public OutputSocket<E> getOutputSocket() {
            return output;
        }

        @Override
        public void flush() throws IOException {
            cache.flush();
        }

        @Override
        public void clear() throws IOException {
            cache.clear();
        }

        class RegisteringInputSocket extends FilterInputSocket<E> {
            RegisteringInputSocket(final InputSocket <? extends E> input) {
                super(input);
            }

            @Override
            public InputStream newInputStream() throws IOException {
                getModel().assertWriteLockedByCurrentThread();
                final InputStream in = getBoundSocket().newInputStream();
                caches.put(path, EntryCache.this);
                return in;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                getModel().assertWriteLockedByCurrentThread();
                final ReadOnlyFile rof = getBoundSocket().newReadOnlyFile();
                caches.put(path, EntryCache.this);
                return rof;
            }
        } // class RegisteringInputSocket

        class RegisteringOutputSocket extends FilterOutputSocket<E> {
            RegisteringOutputSocket(OutputSocket <? extends E> output) {
                super(output);
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                assert getModel().writeLock().isHeldByCurrentThread();

                final OutputStream out = getBoundSocket().newOutputStream();
                // Create marker entry and mind CREATE_PARENTS!
                //controller.mknod(path, FILE, outputOptions, null);
                //getModel().setTouched(true);
                caches.put(path, EntryCache.this);
                return out;
            }
        } // class RegisteringOutputSocket
    } // class EntryCache
}
