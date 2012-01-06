/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * This I/O pool creates and deletes temporary files as {@link FileEntry}s.
 * Besides a call to {@link #release}, the {@link Object#finalize()} method of
 * a created {@link FileEntry} will delete the temporary file, too.
 * However, for best performance you should not rely on this.
 *
 * @since   TrueZIP 7.2
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
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
    private final @Nullable String name;

    /** Constructs a new temp file pool. */
    TempFilePool(
            final @CheckForNull Path dir,
            @CheckForNull String name) {
        this.dir = null != dir ? dir : TEMP_DIR;
        // See http://java.net/jira/browse/TRUEZIP-152
        name = null == name ? "tzp" : name + ".";
        this.name = name;
    }

    @Override
    public TempEntry allocate() throws IOException {
        return new TempEntry(createTempFile(dir, name, null), this);
    }

    @Override
    public void release(Entry<FileEntry> resource) throws IOException {
        resource.release();
    }

    /** A temp file pool entry. */
    @NotThreadSafe
    private static final class TempEntry
    extends FileEntry
    implements Entry<FileEntry> {

        TempEntry(Path file, final TempFilePool pool) {
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
    } // PoolEntry
}
