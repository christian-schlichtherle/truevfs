/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.fs.option.FsInputOption;
import de.truezip.kernel.io.InputException;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.rof.DecoratingReadOnlyFile;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.util.BitField;
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
        final IOBuffer<?> temp;
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
                super(temp.getInputSocket().newReadOnlyFile()); // bind(*) is considered redundant for IOPool.IOBuffer
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