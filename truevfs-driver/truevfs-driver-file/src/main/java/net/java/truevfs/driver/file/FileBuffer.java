/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.truevfs.driver.file;

import java.io.IOException;
import static java.nio.file.Files.deleteIfExists;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A temp file pool entry.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FileBuffer extends FileNode {

    FileBuffer(final Path path, final FileBufferPool pool) {
        super(path);
        assert null != pool;
        this.pool = pool;
    }

    @Override
    public void release() throws IOException {
        if (null == pool)
            throw new IllegalStateException(getPath() + " (already released)");
        pool(null);
    }

    private void pool(@CheckForNull final FileBufferPool newPool) throws IOException {
        final FileBufferPool oldPool = this.pool;
        this.pool = newPool;
        if (oldPool != newPool) deleteIfExists(getPath());
    }

    @Override
    @SuppressWarnings(value = "FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            pool(null);
        } finally {
            super.finalize();
        }
    }
}
