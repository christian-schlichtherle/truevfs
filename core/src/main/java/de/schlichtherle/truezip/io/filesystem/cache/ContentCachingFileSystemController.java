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
package de.schlichtherle.truezip.io.filesystem.cache;

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.DecoratingFileSystemController;
import de.schlichtherle.truezip.io.filesystem.concurrent.ConcurrentFileSystemModel;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncWarningException;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.filesystem.file.Cache;
import de.schlichtherle.truezip.io.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.io.socket.IOCache;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.filesystem.InputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

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
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class ContentCachingFileSystemController<
        M extends ConcurrentFileSystemModel,
        C extends FileSystemController<? extends M>>
extends DecoratingFileSystemController<M, C> {

    private final Map<FileSystemEntryName, EntryCache> caches
            = new HashMap<FileSystemEntryName, EntryCache>();

    /**
     * Constructs a new content caching file system controller.
     *
     * @param controller the decorated file system controller.
     */
    public ContentCachingFileSystemController(@NonNull C controller) {
        super(controller);
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FileSystemEntryName name,
            final BitField<InputOption> options) {
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
            final EntryCache cache = caches.get(name);
            if (null == cache && !options.get(InputOption.CACHE))
                return super.getBoundSocket(); // bypass the cache
            return (null != cache ? cache : new EntryCache(name))
                    .configure(options).getInputSocket().bind(this);
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            final FileSystemEntryName name,
            final BitField<OutputOption> options,
            final Entry template) {
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

            final EntryCache cache = caches.get(name);
            if (null == cache && !options.get(OutputOption.CACHE)
                    || options.get(OutputOption.APPEND)
                    || null != template) {
                if (null != cache) {
                    getModel().assertWriteLockedByCurrentThread();
                    try {
                        cache.flush();
                    } finally {
                        final EntryCache cache2 = caches.remove(name);
                        assert cache == cache2;
                        cache.clear();
                    }
                }
                return super.getBoundSocket(); // bypass the cache
            }
            // Create marker entry and mind CREATE_PARENTS!
            delegate.mknod(name, FILE, options, template);
            getModel().setTouched(true);
            return (null != cache ? cache : new EntryCache(name))
                    .configure(options, template).getOutputSocket().bind(this);
        }
    } // class Output

    @Override
    public void unlink(final FileSystemEntryName name) throws IOException {
        assert getModel().writeLock().isHeldByCurrentThread();

        delegate.unlink(name);
        final EntryCache cache = caches.remove(name);
        if (null != cache)
            cache.clear();
    }

    @Override
    public <X extends IOException>
    void sync(  @NonNull final BitField<SyncOption> options,
                @NonNull final ExceptionBuilder<? super SyncException, X> builder)
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
        delegate.sync(options.clear(CLEAR_CACHE), builder);
    }

    private static final BitField<InputOption> NO_INPUT_OPTIONS
            = BitField.noneOf(InputOption.class);

    private static final BitField<OutputOption> NO_OUTPUT_OPTIONS
            = BitField.noneOf(OutputOption.class);

    private final class EntryCache implements IOCache<Entry> {
        final FileSystemEntryName name;
        final Cache<Entry> cache;
        volatile InputSocket<Entry> input;
        volatile OutputSocket<Entry> output;

        EntryCache(@NonNull final FileSystemEntryName name) {
            this.name = name;
            this.cache = Cache.Strategy.WRITE_BACK.create(Entry.class);
            configure(NO_INPUT_OPTIONS);
            configure(NO_OUTPUT_OPTIONS, null);
        }

        @NonNull
        public EntryCache configure(@NonNull BitField<InputOption> options) {
            cache.configure(new RegisteringInputSocket(delegate.getInputSocket(
                    name, options.clear(InputOption.CACHE))));
            input = null;
            return this;
        }

        @NonNull
        public EntryCache configure(@NonNull BitField<OutputOption> options,
                                    @Nullable Entry template) {
            cache.configure(delegate.getOutputSocket(
                    name, options.clear(OutputOption.CACHE), template));
            output = null;
            return this;
        }

        @Override
        public InputSocket<Entry> getInputSocket() {
            return null != input
                    ? input
                    : (input = cache.getInputSocket());
        }

        @Override
        public OutputSocket<Entry> getOutputSocket() {
            return null != output
                    ? output
                    : (output = new RegisteringOutputSocket(cache.getOutputSocket()));
        }

        @Override
        public void flush() throws IOException {
            cache.flush();
        }

        @Override
        public void clear() throws IOException {
            cache.clear();
        }

        final class RegisteringInputSocket extends DecoratingInputSocket<Entry> {
            RegisteringInputSocket(final InputSocket <?> input) {
                super(input);
            }

            @Override
            public InputStream newInputStream() throws IOException {
                getModel().assertWriteLockedByCurrentThread();
                final InputStream in = getBoundSocket().newInputStream();
                caches.put(name, EntryCache.this);
                return in;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                getModel().assertWriteLockedByCurrentThread();
                final ReadOnlyFile rof = getBoundSocket().newReadOnlyFile();
                caches.put(name, EntryCache.this);
                return rof;
            }
        } // class RegisteringInputSocket

        final class RegisteringOutputSocket extends DecoratingOutputSocket<Entry> {
            RegisteringOutputSocket(OutputSocket <?> output) {
                super(output);
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                assert getModel().writeLock().isHeldByCurrentThread();

                final OutputStream out = getBoundSocket().newOutputStream();
                // Create marker entry and mind CREATE_PARENTS!
                //controller.mknod(name, FILE, outputOptions, null);
                //getModel().setTouched(true);
                caches.put(name, EntryCache.this);
                return out;
            }
        } // class RegisteringOutputSocket
    } // class EntryCache
}
