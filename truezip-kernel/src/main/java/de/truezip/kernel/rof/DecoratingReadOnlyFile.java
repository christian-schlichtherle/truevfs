/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.rof;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

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
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DecoratingReadOnlyFile extends AbstractReadOnlyFile {

    /** The nullable decorated read only file. */
    protected @Nullable ReadOnlyFile rof;

    /**
     * Constructs a new decorating read only file.
     *
     * @param rof the nullable read only file to decorate.
     */
    @CreatesObligation
    protected DecoratingReadOnlyFile(
            final @Nullable @WillCloseWhenClosed ReadOnlyFile rof) {
        this.rof = rof;
    }

    @Override
    public long length() throws IOException {
        return rof.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        return rof.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        rof.seek(pos);
    }

    @Override
    public int read() throws IOException {
        return rof.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return rof.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        rof.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[rof=%s]",
                getClass().getName(),
                rof);
    }
}