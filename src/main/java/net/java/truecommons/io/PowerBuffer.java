/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Adapts a {@link ByteBuffer} to provide an enhanced API, e.g. for reading
 * unsigned integers.
 * This class is a drop-in replacement for {@code ByteBuffer}, so you can use
 * a {@code PowerBuffer} wherever you would otherwise use a
 * {@code ByteBuffer}.
 *
 * @param  <This> The type of this power buffer.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("unchecked")
public abstract class PowerBuffer<This extends PowerBuffer<This>>
implements Comparable<This>, Cloneable {

    @SuppressWarnings("PackageVisibleField")
    ByteBuffer bb;

    //
    // Construction:
    //

    PowerBuffer(final ByteBuffer bb) { this.bb = bb; }

    /** @see MutableBuffer#allocateDirect(int) */
    public static PowerBuffer<?> allocateDirect(int capacity) {
        return MutableBuffer.allocateDirect(capacity);
    }

    /** @see MutableBuffer#allocate(int) */
    public static PowerBuffer<?> allocate(int capacity) {
        return MutableBuffer.allocate(capacity);
    }

    /** @see MutableBuffer#wrap(byte[]) */
    public static PowerBuffer<?> wrap(byte[] array) {
        return MutableBuffer.wrap(array);
    }

    /** @see MutableBuffer#wrap(byte[], int, int) */
    public static PowerBuffer<?> wrap(byte[] array, int offset, int length) {
        return MutableBuffer.wrap(array, offset, length);
    }

    /** @see MutableBuffer#wrap(ByteBuffer) */
    public static PowerBuffer<?> wrap(ByteBuffer buffer) {
        return MutableBuffer.wrap(buffer);
    }

    //
    // PowerBuffer API:
    //

    /** Returns {@code true} if and only if this power buffer is mutable. */
    public abstract boolean isMutable();

    /**
     * Returns a mutable buffer view of the adapted byte buffer.
     * The properties of the returned buffer are equal to the properties of
     * this buffer, including its byte {@link #order()}.
     */
    public abstract MutableBuffer asMutableBuffer();

    /**
     * Returns an immutable buffer view of the adapted byte buffer.
     * The properties of the returned buffer are equal to the properties of
     * this buffer, including its byte {@link #order()}.
     */
    public abstract ImmutableBuffer asImmutableBuffer();

    /**
     * Like {@link #duplicate()} but retains the byte oder, too.
     *
     * @return A {@link #duplicate()} with the same byte order.
     */
    @Override
    public This clone() {
        final This clone;
        try {
            clone = (This) super.clone();
        } catch (final CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
        clone.bb = clone(bb);
        return clone;
    }

    /**
     * Returns the adapted byte buffer.
     *
     * @return The adapted byte buffer.
     */
    public ByteBuffer buffer() { return bb; }

    /**
     * Sets the byte order to little endian.
     *
     * @return {@code this}.
     */
    public This littleEndian() {
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return (This) this;
    }

    /**
     * Sets the byte order to big endian.
     *
     * @return {@code this}.
     */
    public This bigEndian() {
        bb.order(ByteOrder.BIG_ENDIAN);
        return (This) this;
    }

    /**
     * Marks this buffer, reads all remaining bytes from the given channel and
     * resets this buffer.
     * If an {@link IOException} occurs or the end-of-file is reached before
     * this buffer has been entirely filled, then it does not get reset and the
     * {@code IOException} or an {@link EOFException} gets thrown respectively.
     *
     * @param  channel the channel.
     * @return {@code this}.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    public This load(ReadableByteChannel channel)
    throws EOFException, IOException {
        int remaining = bb.remaining();
        bb.mark();
        do {
            int read = channel.read(bb);
            if (0 > read) {
                throw new EOFException();
            }
            remaining -= read;
        } while (0 < remaining);
        bb.reset();
        return (This) this;
    }

    /**
     * Marks this buffer, writes all remaining bytes to the given channel and
     * resets this buffer.
     * If an {@link IOException} occurs, then this buffer does not get reset
     * and the {@code IOException} gets thrown.
     *
     * @param  channel the channel.
     * @return {@code this}.
     * @throws IOException on any I/O error.
     */
    public This save(WritableByteChannel channel) throws IOException {
        int remaining = bb.remaining();
        bb.mark();
        do {
            remaining -= channel.write(bb);
        } while (0 < remaining);
        bb.reset();
        return (This) this;
    }

    /**
     * Skips the given number of bytes.
     * This is a relative move operation to the buffer's position.
     *
     * @param  skip the number of bytes to move forwards.
     *         May be negative to move backwards, too.
     * @return {@code this}.
     * @throws IllegalArgumentException if attempting to move outside the
     *         buffer bounds.
     */
    public This skip(int skip) {
        bb.position(bb.position() + skip);
        return (This) this;
    }

    /**
     * Reads an unsigned byte from the current position.
     *
     * @return The unsigned byte, cast to an integer.
     */
    public int getUByte() { return bb.get() & 0xff; }

    /**
     * Reads an unsigned byte from the given position.
     *
     * @param  index the index position.
     * @return The unsigned byte, cast to an integer.
     */
    public final int getUByte(int index) { return bb.get(index) & 0xff; }

    /**
     * Reads an unsigned short from the current position.
     *
     * @return The unsigned short, cast to an integer.
     */
    public int getUShort() { return bb.getShort() & 0xffff; }

    /**
     * Reads an unsigned short from the given position.
     *
     * @param  index the index position.
     * @return The unsigned short, cast to an integer.
     */
    public final int getUShort(int index) {
        return bb.getShort(index) & 0xffff;
    }

    /**
     * Reads an unsigned int from the current position.
     *
     * @return The unsigned int, cast to a long.
     */
    public long getUInt() { return bb.getInt() & 0xffff_ffffL; }

    /**
     * Reads an unsigned int from the given position.
     *
     * @param  index the index position.
     * @return The unsigned int, cast to a long.
     */
    public final long getUInt(int index) {
        return bb.getInt(index) & 0xffff_ffffL;
    }

    //
    // ByteBuffer API:
    //

    /** @see ByteBuffer#position() */
    public final int position() { return bb.position(); }

    /** @see ByteBuffer#limit() */
    public final int limit() { return bb.limit(); }

    /** @see ByteBuffer#capacity() */
    public final int capacity() { return bb.capacity(); }

    /** @see ByteBuffer#position(int) */
    public This position(int newPosition) {
        bb.position(newPosition);
        return (This) this;
    }

    /** @see ByteBuffer#limit(int) */
    public This limit(int newLimit) {
        bb.limit(newLimit);
        return (This) this;
    }

    /** @see ByteBuffer#mark() */
    public This mark() {
        bb.mark();
        return (This) this;
    }

    /** @see ByteBuffer#reset() */
    public This reset() {
        bb.reset();
        return (This) this;
    }

    /** @see ByteBuffer#clear() */
    public This clear() {
        bb.clear();
        return (This) this;
    }

    /** @see ByteBuffer#flip() */
    public This flip() {
        bb.flip();
        return (This) this;
    }

    /** @see ByteBuffer#rewind() */
    public This rewind() {
        bb.rewind();
        return (This) this;
    }

    /** @see ByteBuffer#remaining() */
    public final int remaining() { return bb.remaining(); }

    /** @see ByteBuffer#hasRemaining() */
    public final boolean hasRemaining() { return bb.hasRemaining(); }

    /** @see ByteBuffer#isReadOnly() */
    public final boolean isReadOnly() { return bb.isReadOnly(); }

    /** @see ByteBuffer#hasArray() */
    public final boolean hasArray() { return bb.hasArray(); }

    /** @see ByteBuffer#array() */
    public byte[] array() { return bb.array(); }

    /** @see ByteBuffer#arrayOffset() */
    public final int arrayOffset() { return bb.arrayOffset(); }

    /** @see ByteBuffer#isDirect() */
    public final boolean isDirect() { return bb.isDirect(); }

    /** @see ByteBuffer#slice() */
    public abstract This slice();

    /** @see ByteBuffer#duplicate() */
    public abstract This duplicate();

    /** @see ByteBuffer#asReadOnlyBuffer() */
    public abstract This asReadOnlyBuffer();

    /** @see ByteBuffer#get() */
    public byte get() { return bb.get(); }

    /** @see ByteBuffer#put(byte) */
    public This put(byte b) {
        bb.put(b);
        return (This) this;
    }

    /** @see ByteBuffer#get(int) */
    public final byte get(int index) { return bb.get(index); }

    /** @see ByteBuffer#put(int, byte) */
    public final This put(int index, byte b) {
        bb.put(index, b);
        return (This) this;
    }

    /** @see ByteBuffer#get(byte[], int, int) */
    public This get(byte[] dst, int offset, int length) {
        bb.get(dst, offset, length);
        return (This) this;
    }

    /** @see ByteBuffer#get(byte[]) */
    public This get(byte[] dst) {
        bb.get(dst);
        return (This) this;
    }

    /**
     * Obtains the adapted byte {@link #buffer()} from the given power buffer
     * and forwards the call to {@link #put(ByteBuffer)}.
     *
     * @param  src the power buffer with the contents to put into this power
     *         buffer.
     * @return {@code this}
     */
    public final This put(PowerBuffer<?> src) { return put(src.buffer()); }

    /** @see ByteBuffer#put(ByteBuffer) */
    public This put(ByteBuffer src) {
        bb.put(src);
        return (This) this;
    }

    /** @see ByteBuffer#put(byte[], int, int) */
    public This put(byte[] src, int offset, int length) {
        bb.put(src, offset, length);
        return (This) this;
    }

    /** @see ByteBuffer#put(byte[]) */
    public This put(byte[] src) {
        bb.put(src);
        return (This) this;
    }

    /** @see ByteBuffer#compact() */
    public This compact() {
        bb.compact();
        return (This) this;
    }

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public final String toString() {
        return String.format("%s[position=%d, limit=%d, capacity=%d]",
                getClass().getName(),
                position(),
                limit(),
                capacity());
    }

    /**
     * Obtains the adapted byte {@link #buffer()} from the given power buffer
     * and forwards the call to {@link ByteBuffer#compareTo(ByteBuffer)}.
     *
     * @param  that the power buffer to compare with this power buffer.
     * @return whether the adapted byte buffer compares less than, equal to or
     *         greater than the adapted byte buffer of the given power buffer.
     */
    @Override
    public final int compareTo(This that) {
        return this.bb.compareTo(that.buffer());
    }

    /** @see ByteBuffer#hashCode() */
    @Override
    public final int hashCode() { return bb.hashCode(); }

    /**
     * Returns {@code true} if and only if the given object is an instance of
     * this class and this power buffer's adapted byte buffer compares
     * {@linkplain ByteBuffer#equals(Object) equal} with that power buffer's
     * adapted byte buffer.
     *
     * @param that the object to test for equality.
     */
    @Override
    public final boolean equals(Object that) {
        return (This) this == that
                || that instanceof PowerBuffer
                    && this.bb.equals(((PowerBuffer<?>) that).buffer());
    }

    /** @see ByteBuffer#order() */
    public final ByteOrder order() { return bb.order(); }

    /** @see ByteBuffer#order(ByteOrder) */
    public This order(ByteOrder order) {
        bb.order(order);
        return (This) this;
    }

    /** @see ByteBuffer#getChar() */
    public char getChar() { return bb.getChar(); }

    /** @see ByteBuffer#putChar(char) */
    public This putChar(char value) {
        bb.putChar(value);
        return (This) this;
    }

    /** @see ByteBuffer#getChar(int) */
    public final char getChar(int index) { return bb.getChar(index); }

    /** @see ByteBuffer#putChar(int, char) */
    public final This putChar(int index, char value) {
        bb.putChar(index, value);
        return (This) this;
    }

    /** @see ByteBuffer#asCharBuffer() */
    public final CharBuffer asCharBuffer() { return bb.asCharBuffer(); }

    /** @see ByteBuffer#getShort() */
    public short getShort() { return bb.getShort(); }

    /** @see ByteBuffer#putShort(short) */
    public This putShort(short value) {
        bb.putShort(value);
        return (This) this;
    }

    /** @see ByteBuffer#getShort(int) */
    public final short getShort(int index) { return bb.getShort(index); }

    /** @see ByteBuffer#putShort(int, short) */
    public final This putShort(int index, short value) {
        bb.putShort(index, value);
        return (This) this;
    }

    /** @see ByteBuffer#asShortBuffer() */
    public final ShortBuffer asShortBuffer() { return bb.asShortBuffer(); }

    /** @see ByteBuffer#getInt() */
    public int getInt() { return bb.getInt(); }

    /** @see ByteBuffer#putInt(int) */
    public This putInt(int value) {
        bb.putInt(value);
        return (This) this;
    }

    /** @see ByteBuffer#getInt(int) */
    public final int getInt(int index) { return bb.getInt(index); }

    /** @see ByteBuffer#putInt(int, int) */
    public final This putInt(int index, int value) {
        bb.putInt(index, value);
        return (This) this;
    }

    /** @see ByteBuffer#asIntBuffer() */
    public final IntBuffer asIntBuffer() { return bb.asIntBuffer(); }

    /** @see ByteBuffer#getLong() */
    public long getLong() { return bb.getLong(); }

    /** @see ByteBuffer#putLong(long) */
    public This putLong(long value) {
        bb.putLong(value);
        return (This) this;
    }

    /** @see ByteBuffer#getLong(int) */
    public final long getLong(int index) { return bb.getLong(index); }

    /** @see ByteBuffer#putLong(int, long) */
    public final This putLong(int index, long value) {
        bb.putLong(index, value);
        return (This) this;
    }

    /** @see ByteBuffer#asLongBuffer() */
    public final LongBuffer asLongBuffer() { return bb.asLongBuffer(); }

    /** @see ByteBuffer#getFloat() */
    public float getFloat() { return bb.getFloat(); }

    /** @see ByteBuffer#putFloat(float) */
    public This putFloat(float value) {
        bb.putFloat(value);
        return (This) this;
    }

    /** @see ByteBuffer#getFloat(int) */
    public final float getFloat(int index) { return bb.getFloat(index); }

    /** @see ByteBuffer#putFloat(int, float) */
    public final This putFloat(int index, float value) {
        bb.putFloat(index, value);
        return (This) this;
    }

    /** @see ByteBuffer#asFloatBuffer() */
    public final FloatBuffer asFloatBuffer() { return bb.asFloatBuffer(); }

    /** @see ByteBuffer#getDouble() */
    public double getDouble() { return bb.getDouble(); }

    /** @see ByteBuffer#putDouble(double) */
    public This putDouble(double value) {
        bb.putDouble(value);
        return (This) this;
    }

    /** @see ByteBuffer#getDouble(int) */
    public final double getDouble(int index) { return bb.getDouble(index); }

    /** @see ByteBuffer#putDouble(int, double) */
    public final This putDouble(int index, double value) {
        bb.putDouble(index, value);
        return (This) this;
    }

    /** @see ByteBuffer#asDoubleBuffer() */
    public final DoubleBuffer asDoubleBuffer() { return bb.asDoubleBuffer(); }

    static ByteBuffer clone(ByteBuffer bb) {
        return bb.duplicate().order(bb.order());
    }
}
