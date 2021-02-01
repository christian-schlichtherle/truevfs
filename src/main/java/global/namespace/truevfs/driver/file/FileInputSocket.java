/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.api.FsAccessOption;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

import static java.nio.file.Files.newByteChannel;
import static java.nio.file.Files.newInputStream;

/**
 * An input socket for a file entry.
 *
 * @see    FileOutputSocket
 * @author Christian Schlichtherle
 */
final class FileInputSocket implements InputSocket<FileNode> {

    private final FileNode node;

    FileInputSocket(
            final BitField<FsAccessOption> unused, // maybe later
            final FileNode node) {
        assert null != node;
        this.node = node;
    }

    @Override
    public FileNode getTarget() {
        return node;
    }

    @Override
    public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer)
    throws IOException {
        return newInputStream(node.getPath());
    }

    @Override
    public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer)
    throws IOException {
        return newByteChannel(node.getPath());
    }
}
