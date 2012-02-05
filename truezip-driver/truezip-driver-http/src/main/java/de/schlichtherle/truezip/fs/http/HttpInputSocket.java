/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An input socket for HTTP(S) entries.
 * 
 * @see     HttpOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class HttpInputSocket extends InputSocket<HttpEntry> {

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
        final IOPool.Entry<?> temp;
        final InputStream in = entry.getInputStream();
        try {
            temp = entry.getPool().allocate();
            try {
                final OutputStream out = temp.getOutputSocket().newOutputStream();
                try {
                    Streams.cat(in, out);
                } finally {
                    out.close();
                }
            } catch (IOException ex) {
                temp.release();
                throw ex;
            }
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                throw new InputException(ex);
            }
        }

        class TempReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            TempReadOnlyFile() throws IOException {
                super(temp.getInputSocket().newReadOnlyFile()); // bind(*) is considered redundant for IOPool.Entry
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                super.close();
                closed = true;
                temp.release();
            }
        } // TempReadOnlyFile

        return new TempReadOnlyFile();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return entry.getInputStream();
    }
}
