/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.zip;

/**
 * Provides static utility methods for reading and writing integer values in
 * little endian format from or to byte arrays.
 * <p>
 * This class is safe for multithreading.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class LittleEndian {

    /** This class cannot get instantiated. */
    private LittleEndian() {
    }

    /**
     * Reads a signed short integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as two bytes, low byte first.
     *
     * @param buf The byte array to read the signed short integer value from.
     * @param off The zero based offset in the byte array where the first byte
     *        of the signed short integer value is read from.
     * @return The signed short integer value read from the byte array.
     */
    public static short readShort(final byte[] buf, final int off) {
        return (short) ((buf[off + 1] << 8) | (buf[off] & 0xff));
    }

    /**
     * Reads an unsigned short integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as two bytes, low byte first.
     * Note that it is <em>not</em> necessary to check the return value with
     * {@link UShort#check}.
     *
     * @param buf The byte array to read the unsigned short integer value from.
     * @param off The zero based offset in the byte array where the first byte
     *        of the unsigned short integer value is read from.
     * @return The unsigned short integer value read from the byte array.
     *         Java does not provide {@code unsigned short} as a primitive
     *         type, hence an {@code int} is returned which's two most
     *         significant bytes are zero.
     */
    public static int readUShort(final byte[] buf, final int off) {
        return ((buf[off + 1] & 0xff) << 8) | (buf[off] & 0xff);
    }

    /**
     * Reads a signed integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as four bytes, low byte first.
     *
     * @param buf The byte array to read the signed integer value from.
     * @param off The zero based offset in the byte array where the first byte
     *        of the signed integer value is read from.
     * @return The signed integer value read from the byte array.
     */
    public static int readInt(final byte[] buf, int off) {
        off += 3;
        int i = buf[off--]; // expands sign
        i <<= 8;
        i |= buf[off--] & 0xff;
        i <<= 8;
        i |= buf[off--] & 0xff;
        i <<= 8;
        i |= buf[off] & 0xff;
        return i;
    }

    /**
     * Reads an unsigned integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as four bytes, low byte first.
     * Note that it is <em>not</em> necessary to check the return value with
     * {@link UInt#check}.
     *
     * @param buf The byte array to read the unsigned integer value from.
     * @param off The zero based offset in the byte array where the first byte
     *        of the unsigned integer value is read from.
     * @return The unsigned integer value read from the byte array.
     *         Java does not provide {@code unsigned int} as a primitive
     *         type, hence a {@code long} is returned which's four most
     *         significant bytes are zero.
     */
    public static long readUInt(final byte[] buf, int off) {
        return readInt(buf, off) & UInt.MAX_VALUE;
    }

    /**
     * Reads a (signed) long integer value from the byte array
     * {@code buf} at the offset {@code off}
     * as eight bytes, low byte first.
     *
     * @param buf The byte array to read the signed long integer value from.
     * @param off The zero based offset in the byte array where the first byte
     *        of the (signed) long integer value is read from.
     * @return The (signed) long integer value read from the byte array.
     */
    public static long readLong(final byte[] buf, int off) {
        off += 7;
        long l = buf[off--]; // expands sign
        l <<= 8;
        l |= buf[off--] & 0xffL;
        l <<= 8;
        l |= buf[off--] & 0xffL;
        l <<= 8;
        l |= buf[off--] & 0xffL;
        l <<= 8;
        l |= buf[off--] & 0xffL;
        l <<= 8;
        l |= buf[off--] & 0xffL;
        l <<= 8;
        l |= buf[off--] & 0xffL;
        l <<= 8;
        l |= buf[off] & 0xffL;
        return l;
    }

    /**
     * Writes the integer value {@code s} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as two bytes, low byte first.
     * The most significant two bytes of the integer value are ignored.
     *
     * @param s The short integer value to be written.
     * @param buf The byte array to write the short integer value to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the short integer value is written to.
     */
    public static void writeShort(int s, final byte[] buf, final int off) {
        buf[off] = (byte) s;
        s >>= 8;
        buf[off + 1] = (byte) s;
    }

    /**
     * Writes the integer value {@code i} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as four bytes, low byte first.
     *
     * @param i The integer value to be written.
     * @param buf The byte array to write the integer value to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the integer value is written to.
     */
    public static void writeInt(int i, final byte[] buf, final int off) {
        buf[off] = (byte) i;
        i >>= 8;
        buf[off + 1] = (byte) i;
        i >>= 8;
        buf[off + 2] = (byte) i;
        i >>= 8;
        buf[off + 3] = (byte) i;
    }

    /**
     * Writes the long integer value {@code l} to the byte array
     * {@code buf} at the zero based offset {@code off}
     * as eight bytes, low byte first.
     *
     * @param l The long integer value to be written.
     * @param buf The byte array to write the long integer value to.
     * @param off The zero based offset in the byte array where the first byte
     *        of the long integer value is written to.
     */
    public static void writeLong(long l, final byte[] buf, final int off) {
        buf[off] = (byte) l;
        l >>= 8;
        buf[off + 1] = (byte) l;
        l >>= 8;
        buf[off + 2] = (byte) l;
        l >>= 8;
        buf[off + 3] = (byte) l;
        l >>= 8;
        buf[off + 4] = (byte) l;
        l >>= 8;
        buf[off + 5] = (byte) l;
        l >>= 8;
        buf[off + 6] = (byte) l;
        l >>= 8;
        buf[off + 7] = (byte) l;
    }
}