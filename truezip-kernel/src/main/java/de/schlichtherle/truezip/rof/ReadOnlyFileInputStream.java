/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.NotThreadSafe;

/**
 * An adapter class turning a provided {@link ReadOnlyFile} into
 * an {@link InputStream}.
 * Note that this stream supports marking.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class ReadOnlyFileInputStream extends InputStream {

    /**
     * The underlying {@link ReadOnlyFile}.
     * Any of the methods in this class throw a {@link NullPointerException}
     * if this hasn't been initialized.
     */
    protected @Nullable ReadOnlyFile rof;

    /**
     * The position of the last mark.
     * Initialized to {@code -1} to indicate that no mark has been set.
     */
    private long mark = -1;

    /**
     * Adapts the given {@code ReadOnlyFile}.
     *
     * @param rof The underlying {@code ReadOnlyFile}. May be
     *        {@code null}, but must be initialized before any method
     *        of this class can be used.
     */
    public ReadOnlyFileInputStream(@Nullable ReadOnlyFile rof) {
        this.rof = rof;
    }

    @Override
    public int read() throws IOException {
        return rof.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return rof.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return rof.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0)
            return 0; // for compatibility to RandomAccessFile

        final long fp = rof.getFilePointer(); // should fail when closed
        final long len = rof.length(); // may succeed when closed
        final long rem = len - fp;
        if (n > rem)
            n = (int) rem;
        rof.seek(fp + n);
        return n;
    }

    @Override
    public int available() throws IOException {
        final long rem = rof.length() - rof.getFilePointer();
        return rem > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
    }

    @Override
    public void close() throws IOException {
        rof.close();
    }

    @Override
    public void mark(int readlimit) {
        try {
            mark = rof.getFilePointer();
        } catch (IOException ex) {
            Logger  .getLogger(ReadOnlyFileInputStream.class.getName())
                    .log(Level.WARNING, ex.getLocalizedMessage(), ex);
            mark = -2;
        }
    }

    @Override
    public void reset() throws IOException {
        if (mark < 0)
            throw new IOException(mark == -1
                    ? "no mark set"
                    : "mark()/reset() not supported by underlying file");
        rof.seek(mark);
    }

    @Override
    public boolean markSupported() {
        try {
            rof.seek(rof.getFilePointer());
            return true;
        } catch (IOException failure) {
            return false;
        }
    }
}
