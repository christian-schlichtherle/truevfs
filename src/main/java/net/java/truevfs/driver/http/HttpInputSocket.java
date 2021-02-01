/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import net.java.truecommons.cio.*;
import net.java.truecommons.io.ReadOnlyChannel;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * An input socket for HTTP(S) entries.
 *
 * @see    HttpOutputSocket
 * @author Christian Schlichtherle
 */
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
    public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
        return entry.newInputStream();
    }

    @Override
    public SeekableByteChannel channel(final Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
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

            BufferReadOnlyChannel() throws IOException {
                super(buffer.input().channel(peer)); // or .channel(Optional.empty())
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
