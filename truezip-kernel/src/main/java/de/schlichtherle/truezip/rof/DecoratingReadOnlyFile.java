/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * An abstract decorator for a read only file.
 * <p>
 * Note that sub-classes of this class may implement their own virtual file
 * pointer.
 * Thus, if you would like to use the decorated read only file again after
 * you have finished using the decorating read only file, then you should not
 * assume a particular position of the file pointer of the decorated read only
 * file.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class DecoratingReadOnlyFile extends AbstractReadOnlyFile {

    /** The nullable decorated read only file. */
    protected @Nullable ReadOnlyFile delegate;

    /**
     * Constructs a new decorating read only file.
     *
     * @param rof the nullable read only file to decorate.
     */
    protected DecoratingReadOnlyFile(final @Nullable ReadOnlyFile rof) {
        this.delegate = rof;
    }

    @Override
    public long length() throws IOException {
        return delegate.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        delegate.seek(pos);
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
