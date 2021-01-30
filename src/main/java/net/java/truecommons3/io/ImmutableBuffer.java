/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A power buffer with immutable properties.
 * This class is a drop-in replacement for {@code ByteBuffer}, so you can use
 * an {@code ImmutableBuffer} wherever you would otherwise use a
 * {@code ByteBuffer}.
 * <p>
 * This class protects the identity and state of its adapted byte buffer,
 * but it does not protect its contents.
 * Any attempt to obtain the adapted byte {@link #buffer()} or change its state
 * results in an {@link UnsupportedOperationException}.
 * However, you may be able to change the adapted byte buffer's content with
 * absolute {@code put} operations unless the buffer is also read-only.
 * <p>
 * This class is designed to be provided as a method parameter where the caller
 * does not want the callee to change the state of the buffer.
 * Consider the following method signature:
 * <pre>{@code
 * void parse(ImmutableBuffer input) throws InvalidDataException;
 * }</pre>
 * <p>
 * Given this signature, it's clear to a caller that the callee cannot change
 * the buffer's state, so it need not worry about that.
 * While a similar effect could get achieved by creating a
 * {@link ByteBuffer#duplicate()} of the buffer before calling this method,
 * using an {@code ImmutableBuffer} avoids the need to document what the callee
 * is allowed or expected to do with the state of the given buffer because
 * its immutable by design.
 * Furthermore, an {@code ImmutableBuffer} can be passed around to any methods
 * without the need to make a protective copy before each call, thereby
 * preventing the pollution of the heap's Eden space and saving some CPU cycles.
 * <p>
 * Mind again that in order to protect the buffer's content, a caller still
 * needs to call {@link #asReadOnlyBuffer()}.
 * <p>
 * This class is immutable.
 *
 * @see    #asMutableBuffer()
 * @since  TrueCommons 2.1
 * @author Christian Schlichtherle
 */
public final class ImmutableBuffer extends PowerBuffer<ImmutableBuffer> {

    //
    // Construction:
    //

    private ImmutableBuffer(ByteBuffer buf) { super(buf); }

    /** @see ByteBuffer#wrap(byte[]) */
    public static ImmutableBuffer wrap(byte[] array) {
        return new ImmutableBuffer(ByteBuffer.wrap(array));
    }

    /** @see ByteBuffer#wrap(byte[], int, int) */
    public static ImmutableBuffer wrap(byte[] array, int offset, int length) {
        return new ImmutableBuffer(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Constructs a new immutable buffer which adapts the given byte
     * {@code buffer}.
     *
     * @param  buf the byte buffer to adapt.
     * @return a new immutable buffer.
     */
    public static ImmutableBuffer wrap(ByteBuffer buf) {
        return new ImmutableBuffer(clone(buf));
    }

    //
    // PowerBuffer API:
    //

    @Override
    public boolean isMutable() { return false; }

    @Override
    public MutableBuffer asMutableBuffer() {
        return MutableBuffer.wrap(clone(bb));
    }

    @Override
    public ImmutableBuffer asImmutableBuffer() { return this; }

    @Override @Deprecated
    public ByteBuffer buffer()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer littleEndian()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer bigEndian()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer load(ReadableByteChannel channel) throws IOException
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer save(WritableByteChannel channel) throws IOException
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer skip(int skip)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public int getUByte()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public int getUShort()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public long getUInt()
    { throw new UnsupportedOperationException(); }

    //
    // ByteBuffer API:
    //

    @Override @Deprecated
    public ImmutableBuffer position(int newPosition)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer limit(int newLimit)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer mark()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer reset()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer clear()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer flip()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer rewind()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public byte[] array()
    { throw new UnsupportedOperationException(); }

    @Override
    public ImmutableBuffer slice() {
        return new ImmutableBuffer(bb.slice());
    }

    @Override
    public ImmutableBuffer duplicate() {
        return new ImmutableBuffer(bb.duplicate());
    }

    @Override
    public ImmutableBuffer asReadOnlyBuffer() {
        return new ImmutableBuffer(bb.asReadOnlyBuffer());
    }

    @Override @Deprecated
    public byte get()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer put(byte b)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer get(byte[] dst, int offset, int length)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer get(byte[] dst)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer put(ByteBuffer src)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer put(byte[] src, int offset, int length)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer put(byte[] src)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer compact()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer order(ByteOrder order)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public char getChar()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer putChar(char value)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public short getShort()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer putShort(short value)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public int getInt()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer putInt(int value)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public long getLong()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer putLong(long value)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public float getFloat()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer putFloat(float value)
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public double getDouble()
    { throw new UnsupportedOperationException(); }

    @Override @Deprecated
    public ImmutableBuffer putDouble(double value)
    { throw new UnsupportedOperationException(); }
}
