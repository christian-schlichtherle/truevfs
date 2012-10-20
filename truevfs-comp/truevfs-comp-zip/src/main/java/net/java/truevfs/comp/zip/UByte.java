/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides constants and static utility methods for unsigned byte integer
 * values ({@value SIZE} bits).
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class UByte {

    /**
     * The minimum value of an unsigned byte integer,
     * which is {@value MIN_VALUE}.
     */
    public static final short MIN_VALUE = 0x00;

    /**
     * The maximum value of an unsigned byte integer,
     * which is {@value MAX_VALUE}.
     */
    public static final short MAX_VALUE = 0xff;

    /**
     * The number of bits used to represent an unsigned short integer in
     * binary form, which is {@value SIZE}.
     */
    public static final int SIZE = 8;

    private UByte() { }

    /**
     * Checks the parameter range.
     *
     * @param  i The integer to check to be in the range of an unsigned byte
     *         integer ({@value SIZE} bits).
     * @param  subject The subject of the exception message
     *         - may be {@code null}.
     *         This should not end with a punctuation character.
     * @param  error First sentence of the exception message
     *         - may be {@code null}.
     *         This should not end with a punctuation character.
     * @return {@code true}
     * @throws IllegalArgumentException If {@code i} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static boolean check(
            final int i,
            final @CheckForNull String subject,
            final @CheckForNull String error) {
        if (MIN_VALUE <= i && i <= MAX_VALUE) return true;
        final StringBuilder message = new StringBuilder();
        if (null != subject) message.append(subject).append(": ");
        if (null != error) message.append(error).append(": ");
        throw new IllegalArgumentException(message
                .append(i)
                .append(" is not within ")
                .append(MIN_VALUE)
                .append(" and ")
                .append(MAX_VALUE)
                .append(" inclusive.")
                .toString());
   }

    /**
     * Checks the parameter range.
     *
     * @param  i The integer to check to be in the range of an unsigned byte
     *         integer ({@value SIZE} bits).
     * @return {@code true}
     * @throws IllegalArgumentException If {@code i} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static boolean check(final int i) {
        return check(i, "Integer out of range", null);
    }
}