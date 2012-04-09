/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.kernel.io.ChannelInputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;

/**
 * An adapter from a seekable byte channel to an input stream which adds a
 * dummy zero byte to the end of the input in order to support
 * {@link ZipInflaterInputStream}.
 *
 * @author Christian Schlichtherle
 */
final class DummyByteChannelInputStream extends ChannelInputStream {
    private boolean added;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    DummyByteChannelInputStream(
            @WillCloseWhenClosed SeekableByteChannel channel) {
        super(channel);
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();
        if (read < 0 && !added) {
            added = true;
            return 0;
        }
        return read;
    }

    @Override
    public int read(final byte[] buf, final int off, int len) throws IOException {
        if (0 == len)
            return 0;
        final int read = super.read(buf, off, len);
        if (read < len && !added) {
            added = true;
            if (read < 0) {
                buf[0] = 0;
                return 1;
            } else {
                buf[read] = 0;
                return read + 1;
            }
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        final int avl = super.available();
        return added || avl >= Integer.MAX_VALUE ? avl : avl + 1;
    }
}