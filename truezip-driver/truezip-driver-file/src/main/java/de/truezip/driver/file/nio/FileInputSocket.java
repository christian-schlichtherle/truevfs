/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.nio;

import de.schlichtherle.truezip.cio.InputSocket;
import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An input socket for a file entry.
 *
 * @since  TrueZIP 7.2
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
