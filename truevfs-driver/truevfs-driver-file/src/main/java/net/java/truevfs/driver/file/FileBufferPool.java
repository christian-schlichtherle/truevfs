/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
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
import net.java.truecommons.cio.IoBufferPool;

/**
 * This I/O pool creates and deletes temporary files as {@link FileBuffer}s.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FileBufferPool extends IoBufferPool {

    private static final Path TEMP_DIR
            = Paths.get(System.getProperty("java.io.tmpdir"));

    private static final FileAttribute<?>[] ATTRIBUTES = isPosix()
            ? posixAttributes()
            : emptyAttributes();

    private static boolean isPosix() {
        return FileSystems
            .getDefault()
            .supportedFileAttributeViews()
            .contains("posix");
    }

    private static FileAttribute<?>[] posixAttributes() {
        return new FileAttribute<?>[] { posixPermissions() };
    }

    private static FileAttribute<?>[] emptyAttributes() {
        return new FileAttribute<?>[0];
    }

    private static FileAttribute<Set<PosixFilePermission>> posixPermissions() {
        return PosixFilePermissions.asFileAttribute(
                EnumSet.of(OWNER_READ, OWNER_WRITE));
    }

    private final @Nullable Path dir;
    private final String prefix;

    FileBufferPool() { this(null, null); }

    FileBufferPool(
            final @CheckForNull Path dir,
            final @CheckForNull String prefix) {
        this.dir = null != dir ? dir : TEMP_DIR;
        this.prefix = null != prefix ? ensureEndsWithDot(prefix) : "tvfs";
    }

    private static String ensureEndsWithDot(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    @Override
    public FileNode allocate() throws IOException {
        // TODO: Fix https://java.net/jira/browse/TRUEVFS-107 .
        return new FileBuffer(createTempFile(dir, prefix, null, attributes()),
                this);
    }

    private static FileAttribute<?>[] attributes() {
        return 0 == ATTRIBUTES.length ? ATTRIBUTES : ATTRIBUTES.clone();
    }
}
