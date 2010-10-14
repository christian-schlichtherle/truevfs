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
package de.schlichtherle.truezip.io.entry;

import de.schlichtherle.truezip.io.Files;
import java.io.File;
import java.io.IOException;

/**
 * This pool creates a temporary file on {@link #allocate()} and deletes it
 * on {@link #release(FileEntry)} or when the returned file entry gets
 * {@link Object#finalize()}d.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TempFilePool implements CommonEntryPool<FileEntry> {

    // Declared package private for unit testing purposes.
    static final String DEFAULT_PREFIX = "tzp-pool";
    static final String DEFAULT_SUFFIX = null;

    private static final TempFilePool instance
            = new TempFilePool(DEFAULT_PREFIX, DEFAULT_PREFIX, null);

    private final String prefix;
    private final String suffix;
    private final File   dir;

    /** Returns the default instance of this temp file pool. */
    public static TempFilePool get() {
        return instance;
    }

    /** Constructs a new temp file pool. */
    public TempFilePool(final String prefix,
                        final String suffix,
                        final File   dir) {
        if (null == prefix)
            throw new NullPointerException();
        this.prefix = prefix;
        this.suffix = suffix;
        this.dir    = dir;
    }

    @Override
    public FileEntry allocate() throws IOException {
        return new TempFileEntry(this,
                Files.createTempFile(prefix, suffix, dir));
    }

    @Override
    public void release(final FileEntry entry) throws IOException {
        if (!(entry instanceof TempFileEntry) ||
                this != ((TempFileEntry) entry).pool(null))
            throw new IllegalArgumentException(entry.getTarget().getPath() + " (not allocated by this temporary file pool)");
    }

    private static final class TempFileEntry extends FileEntry {
        TempFilePool pool;

        TempFileEntry(TempFilePool pool, File file) {
            super(file);
            assert null != pool;
            this.pool = pool;
        }

        TempFilePool pool(final TempFilePool newPool) throws IOException {
            final TempFilePool oldPool = pool;
            this.pool = newPool;
            if (oldPool != newPool)
                if (!getTarget().delete())
                    throw new IOException(getTarget().getPath() + " (cannot delete temporary file)");
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
    }
}
