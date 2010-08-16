/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.io.util;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A stream to write data in Little Endian (LE) format.
 * <p>
 * This class is similar to {@link java.io.DataOutputStream},
 * but writes data in Little Endian format to its underlying stream.
 * A noteable difference to <code>DataOutputStream</code> is that the
 * {@link #size()} method and the {@link #written} field are respectively
 * return <code>long</code> values and wrap to <code>Long.MAX_VALUE</code>.
 * <p>
 * Note that this class is <em>not</em> thread safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class LEDataOutputStream
        extends FilterOutputStream
        implements DataOutput {

    /** This buffer is used for writing data. */
    private final byte[] buf = new byte[8];

    /**
     * The number of bytes written to the data output stream so far. 
     * If this counter overflows, it will be wrapped to Long.MAX_VALUE.
     */
    protected volatile long written;
    
    /**
     * Creates a new data output stream to write data to the specified 
     * underlying output stream. The counter <code>written</code> is 
     * set to zero.
     *
     * @param   out   The underlying output stream, to be saved for later use.
     */
    public LEDataOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Increases the written counter by the specified value
     * until it reaches Long.MAX_VALUE.
     */
    private final void incCount(int inc) {
        final long temp = written + inc;
        written = temp >= 0 ? temp : Long.MAX_VALUE;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument 
     * <code>b</code>) to the underlying output stream.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by one.
     * <p>
     * Implements the <code>write</code> method of <code>OutputStream</code>.
     *
     * @param b The <code>byte</code> value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public void write(int b) throws IOException {
	out.write(b);
        incCount(1);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to the underlying output stream. 
     * If no exception is thrown, the counter <code>written</code> is 
     * incremented by <code>len</code>.
     *
     * @param b The data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @throws IOException If an I/O error occurs.
     */
    public void write(byte b[], int off, int len)
    throws IOException {
	out.write(b, off, len);
	incCount(len);
    }

    /**
     * Writes a <code>boolean</code> value to the underlying output stream
     * as a 1-byte value. The value <code>true</code> is written out as the 
     * value <code>(byte)1</code>; the value <code>false</code> is 
     * written out as the value <code>(byte)0</code>.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by one.
     * 
     * @param b The <code>boolean</code> value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeBoolean(boolean b) throws IOException {
	out.write(b ? 1 : 0);
	incCount(1);
    }

    /**
     * Writes a <code>byte</code> value to the underlying output stream
     * as a 1-byte value.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by one.
     * 
     * @param b The <code>byte</code> value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeByte(int b) throws IOException {
	out.write(b);
        incCount(1);
    }

    /**
     * Writes a <code>char</code> value to the underlying output stream
     * as a 2-byte value, low byte first.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by two.
     * 
     * @param c The <code>char</code> value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeChar(int c) throws IOException {
        writeShort(c);
    }

    /**
     * Writes the integer value <code>s</code> to the underlying output stream
     * as two bytes, low byte first.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by two.
     *
     * @param s The short integer value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeShort(int s) throws IOException {
        buf[0] = (byte) s;
        s >>= 8;
        buf[1] = (byte) s;
        out.write(buf, 0, 2);
        incCount(2);
    }

    /**
     * Writes the integer value <code>i</code> to the underlying output stream
     * as four bytes, low byte first.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by four.
     *
     * @param i The integer value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeInt(int i) throws IOException {
        buf[0] = (byte) i;
        i >>= 8;
        buf[1] = (byte) i;
        i >>= 8;
        buf[2] = (byte) i;
        i >>= 8;
        buf[3] = (byte) i;
        out.write(buf, 0, 4);
        incCount(4);
    }

    /**
     * Writes the integer value <code>l</code> to the underlying output stream
     * as eight bytes, low byte first.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by eight.
     *
     * @param l The long integer value to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeLong(long l) throws IOException {
        buf[0] = (byte) l;
        l >>= 8;
        buf[1] = (byte) l;
        l >>= 8;
        buf[2] = (byte) l;
        l >>= 8;
        buf[3] = (byte) l;
        l >>= 8;
        buf[4] = (byte) l;
        l >>= 8;
        buf[5] = (byte) l;
        l >>= 8;
        buf[6] = (byte) l;
        l >>= 8;
        buf[7] = (byte) l;
        out.write(buf, 0, 8);
	incCount(8);
    }

    /**
     * Converts the float value <code>f</code> to an <code>int</code> using
     * the <code>floatToIntBits</code> method in class <code>Float</code>, 
     * and then writes that <code>int</code> value to the underlying 
     * output stream as a 4-byte quantity, low byte first.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by <code>4</code>.
     * 
     * @param f The <code>float</code> value to be written.
     * @throws IOException If an I/O error occurs.
     * @see java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float f) throws IOException {
	writeInt(Float.floatToIntBits(f));
    }

    /**
     * Converts the double value <code>d</code> to a <code>long</code> using
     * the <code>doubleToLongBits</code> method in class <code>Double</code>, 
     * and then writes that <code>long</code> value to the underlying 
     * output stream as an 8-byte quantity, low byte first.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by <code>8</code>.
     * 
     * @param d The <code>double</code> value to be written.
     * @throws IOException If an I/O error occurs.
     * @see java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double d) throws IOException {
	writeLong(Double.doubleToLongBits(d));
    }

    /**
     * Writes the string <code>s</code> to the underlying output stream as a
     * sequence of bytes. Each character in the string is written out, in 
     * sequence, by discarding its high eight bits.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by the length of <code>s</code>.
     *
     * @param s The string of bytes to be written.
     * @throws IOException If an I/O error occurs.
     */
    public final void writeBytes(String s) throws IOException {
	final int len = s.length();
	for (int i = 0 ; i < len ; i++)
	    writeByte(s.charAt(i));
    }

    /**
     * Writes the string <code>s</code> to the underlying output stream as a
     * sequence of characters. Each character is written out as 
     * if by the <code>writeChar</code> method.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by twice the length of <code>s</code>.
     * 
     * @param s The <code>String</code> value to be written.
     * @throws IOException If an I/O error occurs.
     * @see java.io.DataOutputStream#writeChar(int)
     */
    public final void writeChars(String s) throws IOException {
        final int len = s.length();
        for (int i = 0 ; i < len ; i++)
            writeShort(s.charAt(i));
    }

    /**
     * This method is not implemented.
     *
     * @throws UnsupportedOperationException Always.
     */
    public void writeUTF(String str) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the current value of the counter <code>written</code>, 
     * the number of bytes written to this data output stream so far.
     * If the counter overflows, it will be wrapped to Long.MAX_VALUE.
     * 
     * @return The value of the <code>written</code> field.
     * @see java.io.DataOutputStream#written
     */
    public final long size() {
	return written;
    }
}
