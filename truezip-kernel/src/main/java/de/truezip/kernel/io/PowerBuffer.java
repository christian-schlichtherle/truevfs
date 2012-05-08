/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.truezip.kernel.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * A drop-in replacement which adapts a {@link ByteBuffer} to provide an
 * enhanced API, e.g. for reading unsigned integers.
 * 
 * @author Christian Schlichtherle
 */
public final class PowerBuffer implements Comparable<PowerBuffer> {

    private final ByteBuffer bb;

    //
    // Construction.
    //

    private PowerBuffer(final ByteBuffer bb) {
        this.bb = bb;
    }

    public static PowerBuffer allocateDirect(int capacity) {
        return new PowerBuffer(ByteBuffer.allocateDirect(capacity));
    }

    public static PowerBuffer allocate(int capacity) {
        return new PowerBuffer(ByteBuffer.allocate(capacity));
    }

    public static PowerBuffer wrap(byte[] array, int offset, int length) {
        return new PowerBuffer(ByteBuffer.wrap(array, offset, length));
    }

    public static PowerBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    public static PowerBuffer wrap(ByteBuffer buffer) {
        return new PowerBuffer(Objects.requireNonNull(buffer));
    }

    //
    // PowerBuffer specials.
    //

    /**
     * Returns the adapted byte buffer.
     * 
     * @return The adapted byte buffer.
     */
    public ByteBuffer buffer() {
        return bb;
    }

    /**
     * Sets the byte order to little endian.
     * 
     * @return {@code this}.
     */
    public PowerBuffer littleEndian() {
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return this;
    }

    /**
     * Sets the byte order to big endian.
     * 
     * @return {@code this}.
     */
    public PowerBuffer bigEndian() {
        bb.order(ByteOrder.BIG_ENDIAN);
        return this;
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
    public PowerBuffer load(ReadableByteChannel channel)
    throws EOFException, IOException {
        int remaining = bb.remaining();
        bb.mark();
        do {
            int read = channel.read(bb);
            if (0 > read)
                throw new EOFException();
            remaining -= read;
        } while (0 < remaining);
        bb.reset();
        return this;
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
    public PowerBuffer save(WritableByteChannel channel) throws IOException {
        int remaining = bb.remaining();
        bb.mark();
        do {
            remaining -= channel.write(bb);
        } while (0 < remaining);
        bb.reset();
        return this;
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
    public PowerBuffer skip(int skip) {
        bb.position(bb.position() + skip);
        return this;
    }

    /**
     * Reads an unsigned byte from the current position.
     * 
     * @return The unsigned byte, cast to an integer.
     */
    public int getUByte() {
        return bb.get() & 0xff;
    }

    /**
     * Reads an unsigned byte from the given position.
     * 
     * @return The unsigned byte, cast to an integer.
     */
    public int getUByte(int index) {
        return bb.get(index) & 0xff;
    }

    /**
     * Reads an unsigned short from the current position.
     * 
     * @return The unsigned short, cast to an integer.
     */
    public int getUShort() {
        return bb.getShort() & 0xffff;
    }

    /**
     * Reads an unsigned short from the given position.
     * 
     * @return The unsigned short, cast to an integer.
     */
    public int getUShort(int index) {
        return bb.getShort(index) & 0xffff;
    }

    /**
     * Reads an unsigned int from the current position.
     * 
     * @return The unsigned int, cast to a long.
     */
    public long getUInt() {
        return bb.getInt() & 0xffff_ffffL;
    }

    /**
     * Reads an unsigned int from the given position.
     * 
     * @return The unsigned int, cast to a long.
     */
    public long getUInt(int index) {
        return bb.getInt(index) & 0xffff_ffffL;
    }

    //
    // Plain ByteBuffer API.
    //

    public final int capacity() {
        return bb.capacity();
    }

    public final int position() {
        return bb.position();
    }

    public final PowerBuffer position(int newPosition) {
        bb.position(newPosition);
        return this;
    }

    public final int limit() {
        return bb.limit();
    }

    public final PowerBuffer limit(int newLimit) {
        bb.limit(newLimit);
        return this;
    }

    public final PowerBuffer mark() {
        bb.mark();
        return this;
    }

    public final PowerBuffer reset() {
        bb.reset();
        return this;
    }

    public final PowerBuffer clear() {
        bb.clear();
        return this;
    }

    public final PowerBuffer flip() {
        bb.flip();
        return this;
    }

    public final PowerBuffer rewind() {
        bb.rewind();
        return this;
    }

    public final int remaining() {
        return bb.remaining();
    }

    public final boolean hasRemaining() {
        return bb.hasRemaining();
    }

    public boolean isReadOnly() {
        return bb.isReadOnly();
    }

    public final boolean hasArray() {
        return bb.hasArray();
    }

    public final byte[] array() {
        return bb.array();
    }

    public final int arrayOffset() {
        return bb.arrayOffset();
    }

    public boolean isDirect() {
        return bb.isDirect();
    }

    public PowerBuffer slice() {
        return new PowerBuffer(bb.slice());
    }

    public PowerBuffer duplicate() {
        return new PowerBuffer(bb.duplicate());
    }

    public PowerBuffer asReadOnlyBuffer() {
        return new PowerBuffer(bb.asReadOnlyBuffer());
    }

    public byte get() {
        return bb.get();
    }

    public PowerBuffer put(byte b) {
        bb.put(b);
        return this;
    }

    public byte get(int index) {
        return bb.get(index);
    }

    public PowerBuffer put(int index, byte b) {
        bb.put(index, b);
        return this;
    }

    public PowerBuffer get(byte[] dst, int offset, int length) {
        bb.get(dst, offset, length);
        return this;
    }

    public final PowerBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public PowerBuffer put(PowerBuffer src) {
        bb.put(src.bb);
        return this;
    }

    public PowerBuffer put(byte[] src, int offset, int length) {
        bb.put(src, offset, length);
        return this;
    }

    public final PowerBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    public PowerBuffer compact() {
        bb.compact();
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[position=")
                .append(position())
                .append(", limit=")
                .append(limit())
                .append(", capacity=")
                .append(capacity())
                .append("]")
                .toString();
    }

    @Override
    public int compareTo(PowerBuffer that) {
        return this.bb.compareTo(that.bb);
    }

    @Override
    public int hashCode() {
        return bb.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof PowerBuffer
                    && this.bb.equals(((PowerBuffer) that).bb);
    }

    public final ByteOrder order() {
        return bb.order();
    }

    public final PowerBuffer order(ByteOrder order) {
        bb.order(order);
        return this;
    }

    public char getChar() {
        return bb.getChar();
    }

    public PowerBuffer putChar(char value) {
        bb.putChar(value);
        return this;
    }

    public char getChar(int index) {
        return bb.getChar(index);
    }

    public PowerBuffer putChar(int index, char value) {
        bb.putChar(index, value);
        return this;
    }

    public CharBuffer asCharBuffer() {
        return bb.asCharBuffer();
    }

    public short getShort() {
        return bb.getShort();
    }

    public PowerBuffer putShort(short value) {
        bb.putShort(value);
        return this;
    }

    public short getShort(int index) {
        return bb.getShort(index);
    }

    public PowerBuffer putShort(int index, short value) {
        bb.putShort(index, value);
        return this;
    }

    public ShortBuffer asShortBuffer() {
        return bb.asShortBuffer();
    }

    public int getInt() {
        return bb.getInt();
    }

    public PowerBuffer putInt(int value) {
        bb.putInt(value);
        return this;
    }

    public int getInt(int index) {
        return bb.getInt(index);
    }

    public PowerBuffer putInt(int index, int value) {
        bb.putInt(index, value);
        return this;
    }

    public IntBuffer asIntBuffer() {
        return bb.asIntBuffer();
    }

    public long getLong() {
        return bb.getLong();
    }

    public PowerBuffer putLong(long value) {
        bb.putLong(value);
        return this;
    }

    public long getLong(int index) {
        return bb.getLong(index);
    }

    public PowerBuffer putLong(int index, long value) {
        bb.putLong(index, value);
        return this;
    }

    public LongBuffer asLongBuffer() {
        return bb.asLongBuffer();
    }

    public float getFloat() {
        return bb.getFloat();
    }

    public PowerBuffer putFloat(float value) {
        bb.putFloat(value);
        return this;
    }

    public float getFloat(int index) {
        return bb.getFloat(index);
    }

    public PowerBuffer putFloat(int index, float value) {
        bb.putFloat(index, value);
        return this;
    }

    public FloatBuffer asFloatBuffer() {
        return bb.asFloatBuffer();
    }

    public double getDouble() {
        return bb.getDouble();
    }

    public PowerBuffer putDouble(double value) {
        bb.putDouble(value);
        return this;
    }

    public double getDouble(int index) {
        return bb.getDouble(index);
    }

    public PowerBuffer putDouble(int index, double value) {
        bb.putDouble(index, value);
        return this;
    }

    public DoubleBuffer asDoubleBuffer() {
        return bb.asDoubleBuffer();
    }
}
