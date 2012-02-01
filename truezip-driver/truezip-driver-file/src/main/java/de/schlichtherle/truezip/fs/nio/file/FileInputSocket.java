/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import net.jcip.annotations.ThreadSafe;

/**
 * An input socket for a file entry.
 *
 * @since   TrueZIP 7.2
 * @see     FileOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
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
