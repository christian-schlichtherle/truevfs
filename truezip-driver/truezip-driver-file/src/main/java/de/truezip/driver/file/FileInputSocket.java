/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file;

import de.truezip.kernel.cio.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.Files.newByteChannel;
import static java.nio.file.Files.newInputStream;
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
    public InputStream newStream() throws IOException {
        return newInputStream(entry.getPath());
    }

    @Override
    public SeekableByteChannel newChannel() throws IOException {
        return newByteChannel(entry.getPath());
    }
}
