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
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * This I/O pool creates and deletes temporary files as {@link FileEntry}s.
 * Besides a call to {@link #release}, the {@link Object#finalize()} method of
 * a created {@link FileEntry} will delete the temporary file, too.
 * However, for best performance you should not rely on this.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class TempFilePool implements IOPool<FileEntry> {

    static final TempFilePool INSTANCE = new TempFilePool("tzp", null, null);

    private final @NonNull  String prefix;
    private final @Nullable String suffix;
    private final @Nullable File   dir;

    /** Constructs a new temp file pool. */
    public TempFilePool(final @NonNull  String prefix,
                        final @Nullable String suffix,
                        final @Nullable File dir) {
        if (null == prefix)
            throw new NullPointerException();
        this.prefix = prefix;
        this.suffix = suffix;
        this.dir    = dir;
    }

    @Override
    public Entry allocate() throws IOException {
        return new Entry(this, File.createTempFile(prefix, suffix, dir));
    }

    @Override
    public void release(IOPool.Entry<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    @NotThreadSafe
    public static final class Entry
    extends FileEntry
    implements IOPool.Entry<FileEntry> {

        private TempFilePool pool;

        private Entry(TempFilePool pool, File file) {
            super(file);
            assert null != pool;
            this.pool = pool;
        }

        @Override
        public void release() throws IOException {
            if (null == pool)
                throw new IllegalStateException(getFile() + " (already released)");
            pool(null);
        }

        private TempFilePool pool(final TempFilePool newPool) throws IOException {
            final TempFilePool oldPool = pool;
            this.pool = newPool;
            if (oldPool != newPool) {
                final File file = getFile();
                if (!file.delete() && file.exists())
                    throw new IOException(file + " (cannot delete temporary file)");
            }
            return oldPool;
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                pool(null);
            } finally {
                super.finalize();
            }
        }
    } // class Entry
}
