/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import net.java.truecommons.cio.IoBufferPool;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * This I/O pool creates and deletes temporary files as {@link FileBuffer}s.
 *
 * @author Christian Schlichtherle
 */
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
                EnumSet.of(OWNER_READ, OWNER_WRITE,
                           GROUP_READ, GROUP_WRITE,
                           OTHERS_READ, OTHERS_WRITE));
    }

    private final @Nullable Path dir;
    private final String prefix;

    FileBufferPool() { this(null, null); }

    FileBufferPool(
            final @CheckForNull Path dir,
            final @CheckForNull String prefix) {
        this.dir = null != dir ? dir : TEMP_DIR;
        this.prefix = null != prefix ? prefixPlusDot(prefix) : "tvfs";
    }

    private static String prefixPlusDot(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    @Override
    public FileNode allocate() throws IOException {
        return new FileBuffer(createTempFile(), this);
    }

    private Path createTempFile() throws IOException {
        try {
            return Files.createTempFile(dir, prefix, null, attributes());
        } catch (final IOException ex) {
            if (exists(dir)) throw ex;
            createTempDir();
            return createTempFile();
        }
    }

    private static FileAttribute<?>[] attributes() {
        return 0 == ATTRIBUTES.length ? ATTRIBUTES : ATTRIBUTES.clone();
    }

    private void createTempDir() {
        assert !exists(dir);
        try {
            createDirectories(dir);
        } catch (final IOException ex) {
            // Must NOT map to IOException - see
            // https://java.net/jira/browse/TRUEZIP-321 .
            throw new IllegalArgumentException(dir + " (cannot create directory for temporary files)",
                    ex);
        }
        assert exists(dir);
    }
}
