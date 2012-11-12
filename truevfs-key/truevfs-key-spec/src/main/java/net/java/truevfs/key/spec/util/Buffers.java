/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import static java.nio.charset.CodingErrorAction.*;
import static java.nio.charset.StandardCharsets.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Provides utility methods for encoding and decoding
 * {@linkplain String strings}, {@linkplain CharBuffer char buffers} and char
 * arrays to and from {@linkplain ByteBuffer byte buffers} using the
 * {@linkplain java.nio.charset.StandardCharsets#UTF_8 UTF-8} character set.
 * Note that all allocated buffers are direct buffers.
 *
 * @author Christian Schlichtherle
 */
public final class Buffers {

    private Buffers() { }

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
        try {
            return encode(cb, UTF_8.newEncoder()
                    .onMalformedInput(REPLACE)
                    .onUnmappableCharacter(REPLACE));
        } catch (final CharacterCodingException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    private static ByteBuffer encode(
            final CharBuffer icb,
            final CharsetEncoder enc)
    throws CharacterCodingException {
        int bytes = (int) (icb.remaining() * enc.averageBytesPerChar());
        while (true) {
            final ByteBuffer obb = ByteBuffer.allocateDirect(bytes);
            final CoderResult cr = enc.encode(icb.duplicate(), obb, true);
            if (cr.isUnderflow()) enc.flush(obb);
            obb.flip();
            if (cr.isUnderflow()) return obb;
            if (!cr.isOverflow())  {
                cr.throwException();
                throw new AssertionError();
            }
            fill(obb, (byte) 0);
            bytes = 2 * bytes + 1; // ensure progress
        }
    }

    public static @Nullable String string(
            final @CheckForNull ByteBuffer bb) {
        return null == bb ? null : charBuffer(bb).toString();
    }

    public static @Nullable char[] charArray(
            final @CheckForNull ByteBuffer bb) {
        if (null == bb) return null;
        final CharBuffer ocb = charBuffer(bb);
        final char[] oca = new char[ocb.remaining()];
        ocb.duplicate().get(oca);
        fill(ocb, (char) 0);
        return oca;
    }

    public static @Nullable CharBuffer charBuffer(
            final @CheckForNull ByteBuffer bb) {
        if (null == bb) return null;
        final CharsetDecoder dec = UTF_8.newDecoder()
            .onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE);
        try {
            return decode(bb, dec);
        } catch (final CharacterCodingException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    private static CharBuffer decode(ByteBuffer ibb, CharsetDecoder dec)
    throws CharacterCodingException {
        int bytes = (int) (2 * ibb.remaining() * dec.averageCharsPerByte());
        while (true) {
            final CharBuffer ocb = ByteBuffer
                    .allocateDirect(bytes)
                    .asCharBuffer();
            final CoderResult cr = dec.decode(ibb.duplicate(), ocb, true);
            if (cr.isUnderflow()) dec.flush(ocb);
            ocb.flip();
            if (cr.isUnderflow()) return ocb;
            if (!cr.isOverflow())  {
                cr.throwException();
                throw new AssertionError();
            }
            fill(ocb, (char) 0);
            bytes = 2 * bytes + 2; // ensure progress
        }
    }

    /**
     * Overwrites the remaining bytes of the given byte buffer with the
     * given value.
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
     * Overwrites the remaining characters of the given char buffer with the
     * given value.
     *
     * @param cb the char buffer to fill.
     *        The properties of this buffer remain unchanged.
     * @param value the char value to use for filling the buffer.
     */
    public static void fill(
            final @CheckForNull CharBuffer cb,
            final char value) {
        if (null == cb) return;
        final int position = cb.position();
        final int limit = cb.limit();
        for (int i = position; i < limit; i++) cb.put(i, value);
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
