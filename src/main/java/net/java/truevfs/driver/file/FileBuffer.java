/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.deleteIfExists;

/**
 * A pooled file buffer.
 *
 * @see    FileBufferPool
 * @author Christian Schlichtherle
 */
final class FileBuffer extends FileNode {

    FileBuffer(final Path path, final FileBufferPool pool) {
        super(path);
        assert null != pool;
        this.pool = pool;
    }

    @Override public void release() throws IOException { pool(null); }

    private void pool(@CheckForNull final FileBufferPool newPool) throws IOException {
        final FileBufferPool oldPool = this.pool;
        this.pool = newPool;
        if (oldPool != newPool) deleteIfExists(getPath());
    }

    @Override
    @SuppressWarnings({ "FinalizeDeclaration", "deprecation" })
    protected void finalize() throws Throwable {
        try {
            pool(null);
        } finally {
            super.finalize();
        }
    }
}
