/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * An input socket for HTTP(S) entries.
 * 
 * @see     HttpOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class HttpInputSocket extends InputSocket<HttpEntry> {

    private final HttpEntry entry;

    HttpInputSocket(final HttpEntry                entry, 
                    final BitField<FsInputOption> options) {
        assert null != entry;
        assert null != options;
        this.entry = entry;
    }

    @Override
    public HttpEntry getLocalTarget() {
        return entry;
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        final IOPool.Entry<?> temp = entry.getPool().allocate();
        try {
            Streams.copy(   entry.getConnection().getInputStream(),
                            temp.getOutputSocket().newOutputStream());
        } catch (IOException ex) {
            temp.release();
            throw ex;
        }

        class TempReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            TempReadOnlyFile() throws IOException {
                super(temp.getInputSocket().newReadOnlyFile()); // bind(*) is considered redundant for IOPool.Entry
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    super.close();
                } finally {
                    temp.release();
                }
            }
        } // TempReadOnlyFile

        return new TempReadOnlyFile();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return entry.getConnection().getInputStream();
    }
}
