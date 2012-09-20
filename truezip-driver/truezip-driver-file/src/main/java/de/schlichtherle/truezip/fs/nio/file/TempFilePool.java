/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.socket.IOPool;
import java.io.IOException;
import java.nio.file.FileSystems;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermission.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
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

    private static final boolean IS_POSIX = FileSystems
            .getDefault()
            .supportedFileAttributeViews()
            .contains("posix");
    private static final FileAttribute<Set<PosixFilePermission>>
            POSIX_PERMISSIONS = PosixFilePermissions.asFileAttribute(
                EnumSet.of( OWNER_READ, OWNER_WRITE,
                            GROUP_READ, GROUP_WRITE,
                            OTHERS_READ, OTHERS_WRITE));
    private static final FileAttribute<?>[]
            POSIX_ATTRIBUTES = new FileAttribute<?>[] { POSIX_PERMISSIONS };
    private static final FileAttribute<?>[]
            NO_ATTRIBUTES = new FileAttribute<?>[0];

    /**
     * A default instance of this pool.
     * Use this if you don't have special requirements regarding the temp file
     * prefix, suffix or directory.
     */
    static final TempFilePool INSTANCE = new TempFilePool(null, null);

    private static FileAttribute<?>[] attributes() {
        return IS_POSIX ? POSIX_ATTRIBUTES.clone() : NO_ATTRIBUTES;
    }

    private final @Nullable Path dir;
    private final String name;

    /** Constructs a new temp file pool. */
    TempFilePool(
            final @CheckForNull Path dir,
            final @CheckForNull String name) {
        this.dir = null != dir ? dir : TEMP_DIR;
        // See http://java.net/jira/browse/TRUEZIP-152
        this.name = null != name ? name + "." : "tzp";
    }

    @Override
    public Buffer allocate() throws IOException {
        return new Buffer(createTempFile(dir, name, null, attributes()), this);
    }

    @Override
    public void release(Entry<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    @NotThreadSafe
    private static final class Buffer
    extends FileEntry implements Entry<FileEntry> {

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
