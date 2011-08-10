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
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * A decorating read only file which is limited to read an interval of its
 * decorated read only file.
 * <p>
 * Note that this class implements a virtual file pointer.
 * Thus, if you would like to use the decorated read only file again after
 * you have finished using this decorating read only file, then you should not
 * assume a particular position of the file pointer in the decorated read only
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
    private final boolean exclusive;

    /**
     * The virtual file pointer in the file data.
     * This is relative to {@link #start}.
     */
    private long fp;

    /**
     * Constructs a new interval read only file starting at the current
     * position of the file pointer in the decorated read only file.
     * <p>
     * Note that this constructor assumes that it has exclusive access to the
     * decorated read only file.
     * Concurrent modification of the file pointer in the decorated read only
     * file may corrupt the input of this decorating read only file!
     *
     * @param rof the read only file to decorate.
     * @param length the length of the interval.
     */
    public IntervalReadOnlyFile(
            final @Nullable ReadOnlyFile rof,
            final long length)
    throws IOException {
        this(rof, rof.getFilePointer(), length, true);
    }

    /**
     * Constructs a new interval read only file starting at the given position
     * of the file pointer in the given decorated read only file.
     * <p>
     * Note that this constructor assumes that it does not have exclusive
     * access to the decorated read only file and positions the file pointer
     * in the decorated read only file before each read operation!
     *
     * @param rof the read only file to decorate.
     * @param start the start of the interval.
     * @param length the length of the interval.
     */
    public IntervalReadOnlyFile(
            final @Nullable ReadOnlyFile rof,
            final long start,
            final long length)
    throws IOException {
        this(rof, start, length, false);
    }
    
    /**
     * Constructs a new interval read only file starting at the given position
     * of the file pointer in the given decorated read only file.
     *
     * @param rof the read only file to decorate.
     * @param start the start of the interval.
     * @param length the length of the interval.
     * @param exclusive whether this decorating read only file may assume it
     *        has exclusive access to the decorated read only file or not.
     *        If this is {@code true}, then the position of the file pointer
     *        in the decorated read only file must be {@code start}!
     *        If this is {@code false}, then the file pointer in the decorated
     *        read only file gets positioned before each read operation.
     */
    private IntervalReadOnlyFile(
            final @Nullable ReadOnlyFile rof,
            final long start,
            final long length,
            final boolean exclusive)
    throws IOException {
        super(rof);
        if (start < 0 || length < 0 || rof.length() < start + length)
            throw new IllegalArgumentException();
        this.start = start;
        this.length = length;
        this.exclusive = exclusive;
    }

    @Override
    public long length() throws IOException {
        // Check state.
        final long length = this.length;
        if (this.delegate.length() < this.start + length)
            throw new IOException("Read Only File has been changed!");

        return length;
    }

    @Override
    public long getFilePointer()
    throws IOException {
        // Check state.
        this.delegate.getFilePointer();

        return this.fp;
    }

    @Override
    public void seek(final long fp)
    throws IOException {
        // Check parameters.
        if (fp < 0)
            throw new IOException("File pointer must not be negative!");
        final long length = this.length;
        if (fp > length)
            throw new IOException("File pointer (" + fp
                    + ") is larger than file length (" + length + ")!");

        // Operate.
        this.delegate.seek(fp + this.start);
        this.fp = fp;
    }

    @Override
    public int read()
    throws IOException {
        // Check state.
        long fp = this.fp;
        if (fp >= this.length)
            return -1;

        // Operate.
        if (!this.exclusive)
            this.delegate.seek(fp + this.start);
        final int read = this.delegate.read();

        // Update state.
        this.fp = fp + 1;
        return read;
    }

    @Override
    public int read(final byte[] buf, final int off, int len)
    throws IOException {
        if (0 == len) {
            // Be fault-tolerant and compatible to RandomAccessFile, even if
            // the decorated read only file has been closed before.
            return 0;
        }

        // Check and modify parameters.
        if (0 > (off | len | buf.length - off - len))
	    throw new IndexOutOfBoundsException();
        long fp = this.fp;
        final long length = this.length;
        if (fp + len > length)
            len = (int) (length - fp);

        // Operate.
        if (!this.exclusive)
            this.delegate.seek(fp + this.start);
        final int read = this.delegate.read(buf, off, len);

        // Post-check state.
        if (0 == len) {
            // This was an attempt to read past the end of the file.
            // This could have been checked in advance, but its still desirable
            // to have the delegate test its state - it might throw an
            // IOException if it has been closed before.
            assert 0 >= read;
            return -1;
        }
        assert 0 < read;

        // Update state.
        this.fp = fp + read;
        return read;
    }
}
