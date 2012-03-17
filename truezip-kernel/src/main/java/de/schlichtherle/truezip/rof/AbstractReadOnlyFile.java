/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.rof;

import java.io.EOFException;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract read only file which implements the common boilerplate.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class AbstractReadOnlyFile implements ReadOnlyFile {

    @Override
    public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(final byte[] buf, final int off, final int len)
    throws IOException {
        int total = 0;
        do {
            final int read = read(buf, off + total, len - total);
            if (0 > read)
                throw new EOFException();
            total += read;
        } while (len > total);
    }
}
