/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.cio.IOBuffer;
import de.schlichtherle.truezip.cio.IOPool;
import java.io.File;
import static java.io.File.createTempFile;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This I/O pool creates and deletes temporary files as {@link FileEntry}s.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class TempFilePool implements IOPool<FileEntry> {

    /**
     * A default instance of this pool.
     * Use this if you don't have special requirements regarding the temp file
     * prefix, suffix or directory.
     */
    static final TempFilePool INSTANCE = new TempFilePool(null, null);

    private final @Nullable File dir;
    private final String name;

    /** Constructs a new temp file pool. */
    TempFilePool(
            final @CheckForNull File dir,
            final @CheckForNull String name) {
        this.dir = dir;
        // See http://java.net/jira/browse/TRUEZIP-152
        this.name = null != name ? name + "." : "tzp";
    }

    @Override
    public FileEntry allocate() throws IOException {
        return new FileBuffer(createTempFile(name, null, dir), this);
    }

    @Override
    public void release(IOBuffer<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    @NotThreadSafe
    private static final class FileBuffer
    extends FileEntry
    implements IOBuffer<FileEntry> {

        FileBuffer(File file, final TempFilePool pool) {
            super(file);
            assert null != file;
            assert null != pool;
            this.pool = pool;
        }

        @Override
        public void release() throws IOException {
            if (null == pool)
                throw new IllegalStateException(getFile() + " (already released)");
            pool(null);
        }

        private void pool(final @CheckForNull TempFilePool newPool)
        throws IOException {
            final TempFilePool oldPool = this.pool;
            this.pool = newPool;
            if (oldPool != newPool) {
                final File file = getFile();
                if (!file.delete() && file.exists())
                    throw new IOException(file + " (cannot delete temporary file)");
            }
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
    } // IOBuffer
}
