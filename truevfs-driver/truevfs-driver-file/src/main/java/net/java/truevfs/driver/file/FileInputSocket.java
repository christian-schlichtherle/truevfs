/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.Files.newByteChannel;
import static java.nio.file.Files.newInputStream;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.cio.AbstractInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * An input socket for a file entry.
 *
 * @see    FileOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FileInputSocket extends AbstractInputSocket<FileEntry> {

    private final FileEntry entry;

    FileInputSocket(
            final BitField<FsAccessOption> unused, // maybe later
            final FileEntry entry) {
        assert null != entry;
        this.entry = entry;
    }

    @Override
    public FileEntry target() {
        return entry;
    }

    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return newInputStream(entry.getPath());
    }

    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        return newByteChannel(entry.getPath());
    }
}
