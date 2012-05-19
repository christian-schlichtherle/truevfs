/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.cio.AbstractInputSocket;
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
final class FileInputSocket extends AbstractInputSocket<FileEntry> {

    private final FileEntry entry;

    FileInputSocket(final FileEntry entry) {
        assert null != entry;
        this.entry = entry;
    }

    @Override
    public FileEntry localTarget() {
        return entry;
    }

    @Override
    public InputStream stream() throws IOException {
        return newInputStream(entry.getPath());
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return newByteChannel(entry.getPath());
    }
}
