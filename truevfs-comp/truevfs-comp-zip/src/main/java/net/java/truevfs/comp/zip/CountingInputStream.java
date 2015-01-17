/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.DecoratingInputStream;

/**
 * A decorating input stream which counts the number of bytes read or skipped!
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class CountingInputStream extends DecoratingInputStream {

    /** The number of bytes read. */
    private long bytesRead;

    @CreatesObligation
    CountingInputStream(@WillCloseWhenClosed InputStream in) {
        super(in);
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
    throws IOException {
        final int read = in.read(b, off, len);
        if (read > 0)
            this.bytesRead += read;
        return read;
    }

    @Override
    public int read() throws IOException {
        final int read = in.read();
        if (read != -1)
            this.bytesRead++;
        return read;
    }

    @Override
    public long skip(final long n) throws IOException {
        final long skipped = in.skip(n);
        this.bytesRead += skipped;
        return skipped;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }
}
