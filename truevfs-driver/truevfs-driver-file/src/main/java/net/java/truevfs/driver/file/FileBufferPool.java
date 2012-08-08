/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import java.io.IOException;
import static java.nio.file.Files.createTempFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * This I/O pool creates and deletes temporary files as {@link FileNode}s.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FileBufferPool extends IoBufferPool {

    private static final Path TEMP_DIR
            = Paths.get(System.getProperty("java.io.tmpdir"));

    private final @Nullable Path dir;
    private final String name;

    /**
     * Constructs a default instance of this pool.
     * Use this if you don't have special requirements regarding the temp file
     * prefix, suffix or directory.
     */
    FileBufferPool() {
        this(null, null);
    }

    FileBufferPool(
            final @CheckForNull Path dir,
            final @CheckForNull String name) {
        this.dir = null != dir ? dir : TEMP_DIR;
        // See http://java.net/jira/browse/TRUEZIP-152
        this.name = null != name ? name + "." : "tzp";
    }

    @Override
    public FileNode allocate() throws IOException {
        return new FileBuffer(createTempFile(dir, name, null), this);
    }
}
