/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.truezip.kernel.io;

import java.nio.*;
import javax.annotation.Nullable;

/**
 * A drop-in replacement which adapts a {@link ByteBuffer} to provide
 * additional methods for reading unsigned integers.
 * 
 * @author Christian Schlichtherle
 */
public final class WrappedByteBuffer {

    private @Nullable ByteBuffer bb;

    public WrappedByteBuffer(final ByteBuffer bb) {
        this.bb = bb;
    }

    //
    // WrappedByteBuffer specials.
    //

    /**
     * Returns a {@linkplain ByteBuffer#duplicate() duplicate} of the
     * underlying byte buffer.
     * 
     * @return A {@linkplain ByteBuffer#duplicate() duplicate} of the
     *         underlying byte buffer.
     */
    public ByteBuffer asByteBuffer() {
        return bb.duplicate();
    }

    /**
     * Returns a signed byte, cast to an integer.
     * This is equivalent to {@link #get()}, but with an integer return value.
     * 
     * @return A signed byte, cast to an integer.
     */
    public int getByte() {
        return bb.get();
    }

    /**
     * Returns an unsigned byte, cast to an integer.
     * 
     * @return An unsigned byte, cast to an integer.
     */
    public int getUByte() {
        return bb.get() & 0xff;
    }

    /**
     * Returns an unsigned short, cast to an integer.
     * 
     * @return an unsigned short, cast to an integer.
     */
    public int getUShort() {
        return bb.getShort() & 0xffff;
    }

    /**
     * Returns an unsigned int, cast to a long.
     * 
     * @return An unsigned int, cast to a long.
     */
    public long getUInt() {
        return bb.getInt() & 0xffff_ffffL;
    }

    //
    // Plain ByteBuffer API.
    //

    public WrappedByteBuffer slice() {
        return new WrappedByteBuffer(bb.slice());
    }

    public WrappedByteBuffer duplicate() {
        return new WrappedByteBuffer(bb.duplicate());
    }

    public ByteBuffer asReadOnlyBuffer() {
        return bb.asReadOnlyBuffer();
    }

    public byte get() {
        return bb.get();
    }

    public WrappedByteBuffer put(byte b) {
        bb.put(b);
        return this;
    }

    public byte get(int index) {
        return bb.get(index);
    }

    public WrappedByteBuffer put(int index, byte b) {
        bb.put(index, b);
        return this;
    }

    public WrappedByteBuffer compact() {
        bb.compact();
        return this;
    }

    public boolean isDirect() {
        return bb.isDirect();
    }

    public char getChar() {
        return bb.getChar();
    }

    public WrappedByteBuffer putChar(char value) {
        bb.putChar(value);
        return this;
    }

    public char getChar(int index) {
        return bb.getChar(index);
    }

    public WrappedByteBuffer putChar(int index, char value) {
        bb.putChar(index, value);
        return this;
    }

    public CharBuffer asCharBuffer() {
        return bb.asCharBuffer();
    }

    public short getShort() {
        return bb.getShort();
    }

    public WrappedByteBuffer putShort(short value) {
        bb.putShort(value);
        return this;
    }

    public short getShort(int index) {
        return bb.getShort(index);
    }

    public WrappedByteBuffer putShort(int index, short value) {
        bb.putShort(index, value);
        return this;
    }

    public ShortBuffer asShortBuffer() {
        return bb.asShortBuffer();
    }

    public int getInt() {
        return bb.getInt();
    }

    public WrappedByteBuffer putInt(int value) {
        bb.putInt(value);
        return this;
    }

    public int getInt(int index) {
        return bb.getInt(index);
    }

    public WrappedByteBuffer putInt(int index, int value) {
        bb.putInt(index, value);
        return this;
    }

    public IntBuffer asIntBuffer() {
        return bb.asIntBuffer();
    }

    public long getLong() {
        return bb.getLong();
    }

    public WrappedByteBuffer putLong(long value) {
        bb.putLong(value);
        return this;
    }

    public long getLong(int index) {
        return bb.getLong(index);
    }

    public WrappedByteBuffer putLong(int index, long value) {
        bb.putLong(index, value);
        return this;
    }

    public LongBuffer asLongBuffer() {
        return bb.asLongBuffer();
    }

    public float getFloat() {
        return bb.getFloat();
    }

    public WrappedByteBuffer putFloat(float value) {
        bb.putFloat(value);
        return this;
    }

    public float getFloat(int index) {
        return bb.getFloat(index);
    }

    public WrappedByteBuffer putFloat(int index, float value) {
        bb.putFloat(index, value);
        return this;
    }

    public FloatBuffer asFloatBuffer() {
        return bb.asFloatBuffer();
    }

    public double getDouble() {
        return bb.getDouble();
    }

    public WrappedByteBuffer putDouble(double value) {
        bb.putDouble(value);
        return this;
    }

    public double getDouble(int index) {
        return bb.getDouble(index);
    }

    public WrappedByteBuffer putDouble(int index, double value) {
        bb.putDouble(index, value);
        return this;
    }

    public DoubleBuffer asDoubleBuffer() {
        return bb.asDoubleBuffer();
    }

    public boolean isReadOnly() {
        return bb.isReadOnly();
    }
}
