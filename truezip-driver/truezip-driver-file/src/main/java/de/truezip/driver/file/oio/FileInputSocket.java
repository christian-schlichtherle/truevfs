/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.oio;

import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.rof.DefaultReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An input socket for a file entry.
 *
 * @see    FileOutputSocket
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FileInputSocket extends InputSocket<FileEntry> {

    private final FileEntry entry;

    FileInputSocket(final FileEntry entry) {
        assert null != entry;
        this.entry = entry;
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return new DefaultReadOnlyFile(entry.getFile());
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new FileInputStream(entry.getFile());
    }
}
