/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.io.DecoratingInputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A decorating input stream which counts the number of bytes read.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
final class CountingInputStream extends DecoratingInputStream {

    /** The number of bytes read. */
    private long bytesRead;

    /**
     * Constructs a new counting input stream.
     *
     * @param in the decorated input stream.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    CountingInputStream(@Nullable @WillCloseWhenClosed InputStream in) {
        super(in);
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
    throws IOException {
        final int read = delegate.read(b, off, len);
        this.bytesRead += (read >= 0) ? read : 0;
        return read;
    }

    @Override
    public int read() throws IOException {
        final int read = delegate.read();
        this.bytesRead += -1 != read ? 1 : 0;
        return read;
    }

    @Override
    public long skip(final long n) throws IOException {
        final long skipped = delegate.skip(n);
        this.bytesRead += skipped;
        return skipped;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }

    public void resetBytesRead() {
        this.bytesRead = 0;
    }
}
