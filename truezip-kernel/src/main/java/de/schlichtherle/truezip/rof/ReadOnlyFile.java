/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides random read-only access to a file.
 * The methods of this interface form a subset of {@link RandomAccessFile}
 * which is required for random read-only access.
 * The default implementation can be found in {@link DefaultReadOnlyFile}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public interface ReadOnlyFile extends Closeable {

    /**
     * Returns the length of the file in bytes.
     */
    long length() throws IOException;

    /**
     * Returns the current byte position in the file as a zero-based index.
     */
    long getFilePointer() throws IOException;

    /**
     * Sets the current byte position in the file as a zero-based index at
     * which the next read occurs.
     * Whether the offset may be set beyond the end of the file is up to
     * the implementor.
     * For example, the constructor of the class {@link DefaultReadOnlyFile} 
     * passes {@code "r"} as a parameter to the constructor of its super-class
     * {@link java.io.RandomAccessFile}.
     * With Oracle's JSE implementation, on the Windows platform this
     * implementation allows to seek past the end of file, but on the Linux
     * platform it doesn't.
     *
     * @param pos The current byte position as a zero-based index.
     * @throws IOException If {@code pos} is less than {@code 0} or if
     *         an I/O error occurs.
     */
    void seek(long pos) throws IOException;

    /**
     * Reads and returns the next byte or -1 if the end of the file has been
     * reached.
     */
    int read() throws IOException;

    /**
     * Equivalent to {@link #read(byte[], int, int) read(b, 0, b.length)}.
     */
    int read(byte[] b) throws IOException;

    /**
     * Reads up to {@code len} bytes of data from this read only file into
     * the given array.
     * This method blocks until at least one byte of input is available unless
     * {@code len} is zero.
     *
     * @param  b The buffer to fill with data.
     * @param  off The start offset of the data.
     * @param  len The maximum number of bytes to read.
     * @return The total number of bytes read, or {@code -1} if there is
     *         no more data because the end of the file has been reached.
     * @throws IOException On any I/O related issue.
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Equivalent to {@link #readFully(byte[], int, int) readFully(b, 0, b.length)}.
     */
    void readFully(byte[] b) throws IOException;

    /**
     * Reads {@code len} bytes into the given buffer at the given position.
     *
     * @param  b The buffer to fill with data.
     * @param  off The start offset of the data.
     * @param  len The number of bytes to read.
     * @throws EOFException If less than {@code len} bytes are available
     *         before the end of the file is reached.
     * @throws IOException On any I/O related issue.
     */
    void readFully(byte[] b, int off, int len) throws IOException;

    /**
     * Closes this read-only file and releases any non-heap resources
     * allocated for it.
     */
    @Override
    void close() throws IOException;
}
