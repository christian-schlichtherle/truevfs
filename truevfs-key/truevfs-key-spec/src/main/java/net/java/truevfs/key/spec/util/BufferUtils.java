/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import static java.nio.charset.CodingErrorAction.*;
import static java.nio.charset.StandardCharsets.*;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Provides utility methods for encoding and decoding
 * {@linkplain String strings}, {@linkplain CharBuffer char buffers} and char
 * arrays to and from {@linkplain ByteBuffer byte buffers} using the
 * {@linkplain java.nio.charset.StandardCharsets#UTF_8 UTF-8} character set.
 * Note that all allocated byte buffers are direct byte buffers.
 *
 * @author Christian Schlichtherle
 */
public class BufferUtils {

    private BufferUtils() { }

    public static @Nullable ByteBuffer byteBuffer(
            final @CheckForNull String string) {
        return null == string ? null : byteBuffer(CharBuffer.wrap(string));
    }

    public static @Nullable ByteBuffer byteBuffer(
            final @CheckForNull char[] password) {
        return null == password ? null : byteBuffer(CharBuffer.wrap(password));
    }

    public static @Nullable ByteBuffer byteBuffer(final CharBuffer cb) {
        if (null == cb) return null;
        final CharsetEncoder enc = UTF_8.newEncoder()
                    //.onMalformedInput(REPLACE)
                    //.onUnmappableCharacter(REPLACE)
                    ;
        int capacity = (int) (cb.remaining() * enc.averageBytesPerChar());
        ByteBuffer bb;
        CoderResult cr;
        while (true) {
            bb = ByteBuffer.allocateDirect(capacity);
            cr = enc.encode(cb.duplicate(), bb, true);
            if (cr.isUnderflow()) enc.flush(bb);
            bb.flip();
            if (cr.isUnderflow()) return bb;
            if (!cr.isOverflow()) try {
                cr.throwException();
                throw new AssertionError();
            } catch (final CharacterCodingException ex) {
                throw new IllegalArgumentException(ex);
            }
            fill(bb, (byte) 0);
            capacity = 2 * capacity + 1; // ensure progress
        }
    }

    public static @Nullable String string(
            final @CheckForNull ByteBuffer bb) {
        return null == bb ? null : charBuffer(bb).toString();
    }

    public static @Nullable char[] charArray(
            final @CheckForNull ByteBuffer bb) {
        if (null == bb) return null;
        final CharBuffer cb = charBuffer(bb);
        final char[] array = cb.array();
        final char[] copy = Arrays.copyOfRange(array, 0, cb.remaining());
        Arrays.fill(array, (char) 0);
        return copy;
    }

    public static @Nullable CharBuffer charBuffer(
            final @CheckForNull ByteBuffer bb) {
        if (null == bb) return null;
        return UTF_8.decode(bb.duplicate())/*.flip()*/; // no flipping required
    }

    /**
     * Overwrites the remaining bytes of the given byte buffer with the given
     * value.
     *
     * @param bb the byte buffer to fill.
     *        The properties of this buffer remain unchanged.
     * @param value the byte value to use for filling the buffer.
     */
    public static void fill(
            final @CheckForNull ByteBuffer bb,
            final byte value) {
        if (null == bb) return;
        final int position = bb.position();
        final int limit = bb.limit();
        for (int i = position; i < limit; i++) bb.put(i, value);
    }

    /**
     * Copies the given byte buffer into a new direct byte buffer.
     *
     * @param  bb the byte buffer to copy.
     *         The properties of this buffer remain unchanged.
     * @return the new direct byte buffer with the copied data.
     */
    public static @Nullable ByteBuffer copy(
            final @CheckForNull ByteBuffer bb) {
        return null == bb
                ? null
                : (ByteBuffer) ByteBuffer
                    .allocateDirect(bb.remaining())
                    .put(bb.duplicate())
                    .rewind();
    }
}
