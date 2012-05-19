/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.io;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output stream to write data in Little Endian (LE) format.
 * <p>
 * This class is similar to {@link java.io.DataOutputStream},
 * but writes data in Little Endian format to its underlying stream.
 * A noteable difference to {@code DataOutputStream} is that the
 * {@link #size()} method and the {@link #written} field are respectively
 * return {@code long} values.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class LittleEndianOutputStream extends DecoratingOutputStream {

    /**
     * The number of bytes written to the data output stream so far.
     * If this counter overflows, it will get adjusted to
     * {@link Long#MAX_VALUE}.
     * 
     * @see java.io.DataOutputStream#written
     */
    protected long written;

    /** This buffer is used for writing data. */
    private final byte[] buf = new byte[8];

    protected LittleEndianOutputStream() { }

    public LittleEndianOutputStream(@WillCloseWhenClosed OutputStream out) {
        super(out);
    }

    /**
     * Increases the written counter by the specified value
     * until it reaches {@link Long#MAX_VALUE}.
     */
    private void inc(int inc) {
        final long s = written + inc;
        written = s >= 0 ? s : Long.MAX_VALUE;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * {@code b}) to the underlying output stream.
     * If no exception is thrown, the counter {@code written} is
     * incremented by one.
     * <p>
     * Implements the {@code write} method of {@code OutputStream}.
     *
     * @param b The {@code byte} to be written.
     * @throws IOException on any I/O error.
     */
    @Override
    public void write(int b) throws IOException {
	out.write(b);
        inc(1);
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to the underlying output stream.
     * If no exception is thrown, the counter {@code written} is
     * incremented by {@code len}.
     *
     * @param b The data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @throws IOException on any I/O error.
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
	out.write(b, off, len);
	inc(len);
    }

    /**
     * Writes a {@code boolean} value to the underlying output stream
     * as a 1-byte value. The value {@code true} is written out as the
     * value {@code (byte)1}; the value {@code false} is
     * written out as the value {@code (byte)0}.
     * If no exception is thrown, the counter {@code written} is
     * incremented by one.
     *
     * @param b The {@code boolean} value to be written.
     * @throws IOException on any I/O error.
     */
    public final void writeBoolean(boolean b) throws IOException {
	out.write(b ? 1 : 0);
	inc(1);
    }

    /**
     * Writes a {@code byte} value to the underlying output stream
     * as a 1-byte value.
     * If no exception is thrown, the counter {@code written} is
     * incremented by one.
     *
     * @param b The {@code byte} value to be written.
     * @throws IOException on any I/O error.
     */
    public final void writeByte(int b) throws IOException {
	out.write(b);
        inc(1);
    }

    /**
     * Writes the integer value {@code s} to the underlying output stream
     * as two bytes, low byte first.
     * If no exception is thrown, the counter {@code written} is
     * incremented by two.
     *
     * @param s The short integer value to be written.
     * @throws IOException on any I/O error.
     */
    public final void writeShort(int s) throws IOException {
        buf[0] = (byte) s;
        s >>= 8;
        buf[1] = (byte) s;
        out.write(buf, 0, 2);
        inc(2);
    }

    /**
     * Writes a {@code char} value to the underlying output stream
     * as a 2-byte value, low byte first.
     * If no exception is thrown, the counter {@code written} is
     * incremented by two.
     *
     * @param c The {@code char} value to be written.
     * @throws IOException on any I/O error.
     */
    public final void writeChar(int c) throws IOException {
        writeShort(c);
    }

    /**
     * Writes the integer value {@code i} to the underlying output stream
     * as four bytes, low byte first.
     * If no exception is thrown, the counter {@code written} is
     * incremented by four.
     *
     * @param i The integer value to be written.
     * @throws IOException on any I/O error.
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
        inc(4);
    }

    /**
     * Writes the integer value {@code l} to the underlying output stream
     * as eight bytes, low byte first.
     * If no exception is thrown, the counter {@code written} is
     * incremented by eight.
     *
     * @param l The long integer value to be written.
     * @throws IOException on any I/O error.
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
	inc(8);
    }

    /**
     * Converts the float value {@code f} to an {@code int} using
     * the {@code floatToIntBits} method in class {@code Float},
     * and then writes that {@code int} value to the underlying
     * output stream as a 4-byte quantity, low byte first.
     * If no exception is thrown, the counter {@code written} is
     * incremented by {@code 4}.
     *
     * @param f The {@code float} value to be written.
     * @throws IOException on any I/O error.
     * @see java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float f) throws IOException {
	writeInt(Float.floatToIntBits(f));
    }

    /**
     * Converts the double value {@code d} to a {@code long} using
     * the {@code doubleToLongBits} method in class {@code Double},
     * and then writes that {@code long} value to the underlying
     * output stream as an 8-byte quantity, low byte first.
     * If no exception is thrown, the counter {@code written} is
     * incremented by {@code 8}.
     *
     * @param d The {@code double} value to be written.
     * @throws IOException on any I/O error.
     * @see java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double d) throws IOException {
	writeLong(Double.doubleToLongBits(d));
    }

    /**
     * Writes the string {@code s} to the underlying output stream as a
     * sequence of bytes. Each character in the string is written out, in
     * sequence, by discarding its high eight bits.
     * If no exception is thrown, the counter {@code written} is
     * incremented by the length of {@code s}.
     *
     * @param s The string of bytes to be written.
     * @throws IOException on any I/O error.
     */
    public final void writeBytes(String s) throws IOException {
	final int len = s.length();
	for (int i = 0 ; i < len ; i++)
	    writeByte(s.charAt(i));
    }

    /**
     * Writes the string {@code s} to the underlying output stream as a
     * sequence of characters. Each character is written out as
     * if by the {@code writeChar} method.
     * If no exception is thrown, the counter {@code written} is
     * incremented by twice the length of {@code s}.
     *
     * @param s The {@code String} value to be written.
     * @throws IOException on any I/O error.
     * @see java.io.DataOutputStream#writeChar(int)
     */
    public final void writeChars(String s) throws IOException {
        final int len = s.length();
        for (int i = 0 ; i < len ; i++)
            writeChar(s.charAt(i));
    }

    /**
     * This method is not implemented.
     *
     * @param str the string to write.
     * @throws UnsupportedOperationException Always.
     */
    public void writeUTF(String str) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the current value of the counter {@code written},
     * the number of bytes written to this data output stream so far.
     * If the counter overflows, it will get adjusted to {@link Long#MAX_VALUE}.
     *
     * @return The value of the field {@link #written}.
     * @see    java.io.DataOutputStream#size()
     */
    public final long size() {
        return written;
    }
}
