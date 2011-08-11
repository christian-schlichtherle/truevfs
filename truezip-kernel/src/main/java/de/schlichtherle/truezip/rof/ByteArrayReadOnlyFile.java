/*
 * Copyright 2011 Schlichtherle IT Services
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
 * A read only file which reads from a byte array provided to its constructor.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class ByteArrayReadOnlyFile extends AbstractReadOnlyFile {

    private final byte[] buffer;
    private final int start;
    private int position;
    private int limit;

    /**
     * Constructs a new byte array read only file.
     * 
     * @param buf the array to read from.
     *        Note that this array is <em>not</em> copied, so beware of
     *        concurrent modifications!
     */
    public ByteArrayReadOnlyFile(final byte[] buf) {
        this(buf, 0, buf.length);
    }

    /**
     * Constructs a new byte array read only file.
     *
     * @param buffer the array to read from.
     *        Note that this array is <em>not</em> copied, so beware of
     *        concurrent modifications!
     * @param offset the start of the window to read from the array.
     * @param length the length of the window to read from the array.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP2")
    public ByteArrayReadOnlyFile(byte buffer[], int offset, int length) {
	this.buffer = buffer;
        this.position = this.start = offset;
	this.limit = Math.min(offset + length, buffer.length);
    }

    @Override
    public long length() {
        return limit - start;
    }

    @Override
    public long getFilePointer() {
        return position - start;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (pos < 0)
            throw new IOException();
        pos += start;
        this.position = pos < limit ? (int) pos : limit;
    }

    @Override
    public int read() {
	return position < limit ? buffer[position++] & 0xFF : -1;
    }

    @Override
    public int read(final byte[] buffer, final int offset, int remaining) {
	if (remaining <= 0)
	    return 0;
        final int position = this.position;
        final int available = limit - position;
        if (available <= 0)
            return -1;
        if (remaining > available)
	    remaining = available;
	System.arraycopy(this.buffer, position, buffer, offset, remaining);
	this.position += remaining;
	return remaining;
    }

    @Override
    public void close() throws IOException {
    }
}
