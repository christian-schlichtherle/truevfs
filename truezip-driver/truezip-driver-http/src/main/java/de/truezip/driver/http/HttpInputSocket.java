/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import de.truezip.kernel.io.InputException;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An input socket for HTTP(S) entries.
 * 
 * @see    HttpOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class HttpInputSocket extends InputSocket<HttpEntry> {

    private final HttpEntry entry;

    HttpInputSocket(final HttpEntry                entry, 
                    final BitField<AccessOption> options) {
        assert null != entry;
        assert null != options;
        this.entry = entry;
    }

    @Override
    public HttpEntry getLocalTarget() {
        return entry;
    }

    @Override
    public InputStream newStream() throws IOException {
        return entry.getInputStream();
    }

    @Override
    public SeekableByteChannel newChannel() throws IOException {
        final IOBuffer<?> temp;
        final InputStream in = entry.getInputStream();
        try {
            temp = entry.getPool().allocate();
            try {
                try (final OutputStream out = temp.getOutputSocket().newStream()) {
                    Streams.cat(in, out);
                }
            } catch (final Throwable ex) {
                temp.release();
                throw ex;
            }
        } finally {
            try {
                in.close();
            } catch (final IOException ex) {
                throw new InputException(ex);
            }
        }

        final class TempReadOnlyChannel extends DecoratingReadOnlyChannel {
            boolean closed;

            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            TempReadOnlyChannel() throws IOException {
                super(temp.getInputSocket().newChannel()); // bind(*) is considered redundant for IOPool.IOBuffer
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                channel.close();
                closed = true;
                temp.release();
            }
        } // TempReadOnlyChannel

        return new TempReadOnlyChannel();
    }
}
