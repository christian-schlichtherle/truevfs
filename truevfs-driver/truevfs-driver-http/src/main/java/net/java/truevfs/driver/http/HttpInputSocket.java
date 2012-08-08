/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.ReadOnlyChannel;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.cio.*;

/**
 * An input socket for HTTP(S) entries.
 * 
 * @see    HttpOutputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class HttpInputSocket extends AbstractInputSocket<HttpNode> {

    private final HttpNode entry;

    HttpInputSocket(
            final BitField<FsAccessOption> options,
            final HttpNode entry) {
        assert null != entry;
        assert null != options;
        this.entry = entry;
    }

    @Override
    public HttpNode target() {
        return entry;
    }

    @Override
    public InputStream stream(final OutputSocket<? extends Entry> peer)
    throws IOException {
        return entry.newInputStream();
    }

    @Override
    public SeekableByteChannel channel(final OutputSocket<? extends Entry> peer)
    throws IOException {
        final IoBuffer buffer = entry.getPool().allocate();
        try {
            IoSockets.copy(entry.input(), buffer.output());
        } catch (final Throwable ex) {
            try {
                buffer.release();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        final class BufferReadOnlyChannel extends ReadOnlyChannel {
            boolean closed;

            @CreatesObligation
            BufferReadOnlyChannel() throws IOException {
                super(buffer.input().channel(peer)); // or .channel(null)
            }

            @Override
            public void close() throws IOException {
                if (!closed) {
                    channel.close();
                    closed = true;
                    buffer.release();
                }
            }
        }
        return new BufferReadOnlyChannel();
    }
}
