/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
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
import net.java.truecommons.cio.AbstractInputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.OutputSocket;

/**
 * An input socket for a file entry.
 *
 * @see    FileOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FileInputSocket extends AbstractInputSocket<FileNode> {

    private final FileNode node;

    FileInputSocket(
            final BitField<FsAccessOption> unused, // maybe later
            final FileNode node) {
        assert null != node;
        this.node = node;
    }

    @Override
    public FileNode target() {
        return node;
    }

    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return newInputStream(node.getPath());
    }

    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        return newByteChannel(node.getPath());
    }
}
