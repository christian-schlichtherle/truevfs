/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.http;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;
import net.truevfs.kernel.FsAccessOption;
import net.truevfs.kernel.cio.AbstractInputSocket;
import net.truevfs.kernel.cio.IoBuffer;
import net.truevfs.kernel.cio.IoSockets;
import net.truevfs.kernel.io.AbstractSource;
import net.truevfs.kernel.io.DecoratingReadOnlyChannel;
import net.truevfs.kernel.io.Streams;
import net.truevfs.kernel.util.BitField;

/**
 * An input socket for HTTP(S) entries.
 * 
 * @see    HttpOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class HttpInputSocket extends AbstractInputSocket<HttpEntry> {

    private final HttpEntry entry;

    HttpInputSocket(final HttpEntry                entry, 
                    final BitField<FsAccessOption> options) {
        assert null != entry;
        assert null != options;
        this.entry = entry;
    }

    @Override
    public HttpEntry localTarget() {
        return entry;
    }

    @Override
    public InputStream stream() throws IOException {
        return entry.newInputStream();
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        final IoBuffer<?> temp = entry.getPool().allocate();
        try {
            IoSockets.copy(entry.input(), temp.output());
        } catch (final Throwable ex) {
            try {
                temp.release();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        final class TempReadOnlyChannel extends DecoratingReadOnlyChannel {
            boolean closed;

            @CreatesObligation
            TempReadOnlyChannel() throws IOException {
                super(temp.input().channel()); // bind(*) is considered redundant for IoPool.IoBuffer
            }

            @Override
            public void close() throws IOException {
                if (!closed) {
                    channel.close();
                    closed = true;
                    temp.release();
                }
            }
        }
        return new TempReadOnlyChannel();
    }
}
