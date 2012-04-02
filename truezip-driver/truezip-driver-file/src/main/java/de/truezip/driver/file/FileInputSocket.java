/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file;

import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.rof.DefaultReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An input socket for a file entry.
 *
 * @see    FileOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
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
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return Files.newByteChannel(entry.getPath());
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        // TODO: Write SeekableByteChannel adapter.
        return new DefaultReadOnlyFile(entry.getPath().toFile());
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return Files.newInputStream(entry.getPath());
    }
}