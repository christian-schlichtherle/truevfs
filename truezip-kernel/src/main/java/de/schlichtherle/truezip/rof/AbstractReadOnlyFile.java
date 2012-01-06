/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.EOFException;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * An abstract read only file which implements the common boilerplate.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
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
        int total = 0, read;
        do {
            read = read(buf, off + total, len - total);
            if (read < 0)
                throw new EOFException();
            total += read;
        } while (total < len);
    }
}
