/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
import net.jcip.annotations.ThreadSafe;

/**
 * A read only file which reads from a byte array provided to its constructor.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class ByteArrayReadOnlyFile extends AbstractReadOnlyFile {

    private final byte[] data;
    private int offset;
    private int length;

    /**
     * Constructs a new byte array read only file.
     * 
     * @param data the array to read from.
     *        Note that this array is <em>not</em> copied, so beware of
     *        concurrent modifications!
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP2")
    public ByteArrayReadOnlyFile(final byte [] data) {
        if (null == data)
            throw new NullPointerException();
	this.data = data;
        this.offset = 0;
	this.length = data.length;
    }

    /**
     * Constructs a new byte array read only file.
     *
     * @param data the array to read from.
     *        Note that this array is <em>not</em> copied, so beware of
     *        concurrent modifications!
     * @param offset the start of the window to read from the array.
     * @param length the length of the window to read from the array.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP2")
    public ByteArrayReadOnlyFile(byte data[], int offset, int length) {
        if (null == data)
            throw new NullPointerException();
	this.data = data;
        this.offset = offset;
	this.length = Math.min(offset + length, data.length);
    }

    @Override
    public synchronized long length() {
        return length;
    }

    @Override
    public synchronized long getFilePointer() {
        return offset;
    }

    @Override
    public synchronized void seek(long pos) throws IOException {
        if (0 > pos)
            throw new IOException();
        this.offset = pos < length ? (int) pos : length;
    }

    @Override
    public synchronized int read() {
	return offset < length ? data[offset++] & 0xFF : -1;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) {
	if (0 > (off | len | b.length - off - len))
	    throw new IndexOutOfBoundsException();
	if (0 >= len)
	    return 0;
        if (offset + len > length)
	    len = length - offset;
        if (offset >= length)
            return -1;
	System.arraycopy(data, offset, b, off, len);
	offset += len;
	return len;
    }

    @Override
    public void close() {
    }
}
