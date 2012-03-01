/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.socket.IOPool;
import java.io.IOException;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This I/O pool creates and deletes temporary files as {@link FileEntry}s.
 *
 * @since  TrueZIP 7.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class TempFilePool implements IOPool<FileEntry> {

    private static final Path TEMP_DIR
            = Paths.get(System.getProperty("java.io.tmpdir"));

    /**
     * A default instance of this pool.
     * Use this if you don't have special requirements regarding the temp file
     * prefix, suffix or directory.
     */
    static final TempFilePool INSTANCE = new TempFilePool(null, null);

    private final @Nullable Path dir;
    private final String name;

    /** Constructs a new temp file pool. */
    TempFilePool(
            final @CheckForNull Path dir,
            @CheckForNull String name) {
        this.dir = null != dir ? dir : TEMP_DIR;
        // See http://java.net/jira/browse/TRUEZIP-152
        name = null != name ? name + "." : "tzp";
        this.name = name;
    }

    @Override
    public Buffer allocate() throws IOException {
        return new Buffer(createTempFile(dir, name, null), this);
    }

    @Override
    public void release(Entry<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    @NotThreadSafe
    private static final class Buffer
    extends FileEntry
    implements Entry<FileEntry> {

        Buffer(Path file, final TempFilePool pool) {
            super(file);
            assert null != file;
            assert null != pool;
            this.pool = pool;
        }

        @Override
        public void release() throws IOException {
            if (null == pool)
                throw new IllegalStateException(getPath() + " (already released)");
            pool(null);
        }

        private void pool(final @CheckForNull TempFilePool newPool)
        throws IOException {
            final TempFilePool oldPool = this.pool;
            this.pool = newPool;
            if (oldPool != newPool)
                deleteIfExists(getPath());
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
    } // Buffer
}
