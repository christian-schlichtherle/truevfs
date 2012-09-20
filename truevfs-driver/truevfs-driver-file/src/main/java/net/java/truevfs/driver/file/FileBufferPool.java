/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import java.io.IOException;
import java.nio.file.FileSystems;
import static java.nio.file.Files.createTempFile;
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

    private static FileAttribute<?>[] attributes() {
        return IS_POSIX ? POSIX_ATTRIBUTES.clone() : NO_ATTRIBUTES;
    }

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
        return new FileBuffer(createTempFile(dir, name, null, attributes()),
                this);
    }
}
