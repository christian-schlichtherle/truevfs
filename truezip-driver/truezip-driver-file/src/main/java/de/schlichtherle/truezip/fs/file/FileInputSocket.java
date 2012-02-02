/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.socket.InputSocket;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An input socket for a file entry.
 *
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
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return new DefaultReadOnlyFile(entry.getFile());
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new FileInputStream(entry.getFile());
    }
}
