/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import java.io.IOException;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * This I/O pool creates and deletes temporary files as {@link FileEntry}s.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class TempFilePool implements IoPool<FileEntry> {

    private static final Path TEMP_DIR
            = Paths.get(System.getProperty("java.io.tmpdir"));

    private final @Nullable Path dir;
    private final String name;

    /**
     * Constructs a default instance of this pool.
     * Use this if you don't have special requirements regarding the temp file
     * prefix, suffix or directory.
     */
    TempFilePool() {
        this(null, null);
    }

    TempFilePool(
            final @CheckForNull Path dir,
            final @CheckForNull String name) {
        this.dir = null != dir ? dir : TEMP_DIR;
        // See http://java.net/jira/browse/TRUEZIP-152
        this.name = null != name ? name + "." : "tzp";
    }

    @Override
    public FileEntry allocate() throws IOException {
        return new FileBuffer(createTempFile(dir, name, null), this);
    }

    @Override
    public void release(IoBuffer<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    @NotThreadSafe
    private static final class FileBuffer
    extends FileEntry
    implements IoBuffer<FileEntry> {

        FileBuffer(Path file, final TempFilePool pool) {
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
    } // FileBuffer
}
