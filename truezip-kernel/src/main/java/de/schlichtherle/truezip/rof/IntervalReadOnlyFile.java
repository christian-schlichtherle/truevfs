/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * A read only file which reads a defined interval from its decorated read only
 * file.
 * <p>
 * Note that this class implements a virtual file pointer.
 * Thus, if you would like to use the decorated read only file again after
 * you have finished using the decorating read only file, then you should not
 * assume a particular position of the file pointer of the decorated read only
 * file.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class IntervalReadOnlyFile extends DecoratingReadOnlyFile {

    private final long start;
    private final long length;

    /**
     * The virtual file pointer in the file data.
     * This is relative to the start of the file.
     */
    private long fp;

    /**
     * Constructs a new interval read only file starting at the current
     * position of the file pointer in the decorated read only file.
     *
     * @param rof the read only file to decorate.
     * @param length the length of the interval.
     */
    public IntervalReadOnlyFile(
            final ReadOnlyFile rof,
            final long length)
    throws IOException {
        super(rof);
        final long start = rof.getFilePointer();
        if (start < 0 || length < 0 || rof.length() < start + length)
            throw new IllegalArgumentException();
        this.start = start;
        this.length = length;
    }

    /**
     * Constructs a new interval read only file and positions the file pointer
     * in the decorated read only file at the given start.
     *
     * @param rof the read only file to decorate.
     * @param start the start of the interval.
     * @param length the length of the interval.
     */
    public IntervalReadOnlyFile(
            final ReadOnlyFile rof,
            final long start,
            final long length)
    throws IOException {
        super(rof);
        if (start < 0 || length < 0 || rof.length() < start + length)
            throw new IllegalArgumentException();
        this.start = start;
        this.length = length;
        rof.seek(start);
    }

    /**
     * Asserts that this file is open.
     *
     * @throws IOException If the preconditions do not hold.
     */
    private void assertOpen() throws IOException {
        if (null == delegate)
            throw new IOException("file is closed");
    }

    @Override
    public long length() throws IOException {
        // Check state.
        assertOpen();

        return length;
    }

    @Override
    public long getFilePointer()
    throws IOException {
        // Check state.
        assertOpen();

        return fp;
    }

    @Override
    public void seek(final long fp)
    throws IOException {
        // Check state.
        assertOpen();

        if (fp < 0)
            throw new IOException("File pointer must not be negative!");
        final long length = length();
        if (fp > length)
            throw new IOException("File pointer (" + fp
                    + ") is larger than file length (" + length + ")!");

        delegate.seek(fp + start);
        this.fp = fp;
    }

    @Override
    public int read()
    throws IOException {
        // Check state.
        assertOpen();
        if (fp >= length())
            return -1;

        final int read = delegate.read();
        fp++;
        return read;
    }

    @Override
    public int read(final byte[] buf, final int off, int len)
    throws IOException {
        if (len == 0)
            return 0; // be fault-tolerant and compatible to RandomAccessFile

        // Check state.
        assertOpen();
        final long length = length();
        if (fp >= length)
            return -1;

        // Check parameters.
        if (0 > (off | len | buf.length - off - len))
	    throw new IndexOutOfBoundsException();
        if (fp + len > length)
            len = (int) (length - fp);

        final int read = delegate.read(buf, off, len);
        fp += read;

        // Assert that at least one byte has been read if len isn't zero.
        // Note that EOF has been tested before.
        assert read > 0;
        return read;
    }

    /**
     * Closes this read only file.
     * As a side effect, this will set the reference to the decorated read
     * only file ({@link #delegate} to {@code null}.
     */
    @Override
    public void close()
    throws IOException {
        // Order is important here!
        if (null == delegate)
            return;
        try {
            delegate.close();
        } finally {
            delegate = null;
        }
    }
}
