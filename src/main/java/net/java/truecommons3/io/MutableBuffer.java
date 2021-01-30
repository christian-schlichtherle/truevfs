/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A power buffer with mutable properties.
 * This class is a drop-in replacement for {@code ByteBuffer}, so you can use
 * a {@code MutableBuffer} wherever you would otherwise use a
 * {@code ByteBuffer}.
 * However, unlike the {@code ByteBuffer} class, a clone of a
 * {@code MutableBuffer} always inherits the original buffer's byte order,
 * e.g. when calling {@link #slice()}, {@link #duplicate()} or
 * {@link #asReadOnlyBuffer()}.
 *
 * @see    #asImmutableBuffer()
 * @since  TrueCommons 2.1
 * @author Christian Schlichtherle
 */
public final class MutableBuffer extends PowerBuffer<MutableBuffer> {

    //
    // Construction:
    //

    private MutableBuffer(ByteBuffer buf) { super(buf); }

    /** @see ByteBuffer#allocateDirect(int) */
    public static MutableBuffer allocateDirect(int capacity) {
        return new MutableBuffer(ByteBuffer.allocateDirect(capacity));
    }

    /** @see ByteBuffer#allocate(int) */
    public static MutableBuffer allocate(int capacity) {
        return new MutableBuffer(ByteBuffer.allocate(capacity));
    }

    /** @see ByteBuffer#wrap(byte[]) */
    public static MutableBuffer wrap(byte[] array) {
        return new MutableBuffer(ByteBuffer.wrap(array));
    }

    /** @see ByteBuffer#wrap(byte[], int, int) */
    public static MutableBuffer wrap(byte[] array, int offset, int length) {
        return new MutableBuffer(ByteBuffer.wrap(array, offset, length));
    }

    /**
     * Constructs a new mutable buffer which adapts the given byte
     * {@code buffer}.
     *
     * @param  buf the byte buffer to adapt.
     * @return a new mutable buffer.
     */
    public static MutableBuffer wrap(ByteBuffer buf) {
        return new MutableBuffer(Objects.requireNonNull(buf));
    }

    //
    // PowerBuffer API:
    //

    @Override
    public boolean isMutable() { return true; }

    @Override
    public MutableBuffer asMutableBuffer() { return this; }

    @Override
    public ImmutableBuffer asImmutableBuffer() {
        return ImmutableBuffer.wrap(bb);
    }

    //
    // ByteBuffer API:
    //

    @Override
    public MutableBuffer slice() { return new MutableBuffer(bb.slice()); }

    @Override
    public MutableBuffer duplicate() {
        return new MutableBuffer(bb.duplicate());
    }

    @Override
    public MutableBuffer asReadOnlyBuffer() {
        return new MutableBuffer(bb.asReadOnlyBuffer());
    }
}
