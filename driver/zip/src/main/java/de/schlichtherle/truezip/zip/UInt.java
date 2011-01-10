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
 * Provides constants and static utility methods for unsigned integer
 * values ({@value SIZE} bits).
 * <p>
 * This class is safe for multithreading.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class UInt {

    /**
     * The minimum value of an unsigned integer,
     * which is {@value MIN_VALUE}.
     */
    public static final long MIN_VALUE = 0x00000000L;

    /**
     * The maximum value of an unsigned integer,
     * which is {@value MAX_VALUE}.
     */
    public static final long MAX_VALUE = 0xffffffffL;

    /**
     * The number of bits used to represent an unsigned integer in
     * binary form, which is {@value SIZE}.
     */
    public static final int SIZE = 32;

    /** This class cannot get instantiated. */
    private UInt() {
    }

    /**
     * Checks the parameter range.
     * 
     * @param l The long integer to check to be in the range of an unsigned
     *        integer ({@value SIZE} bits).
     * @param subject The subject of the exception message
     *        - may be {@code null}.
     *        This should not end with a punctuation character.
     * @param error First sentence of the exception message
     *        - may be {@code null}.
     *        This should not end with a punctuation character.
     * @throws IllegalArgumentException If {@code l} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static void check(
            final long l,
            final String subject,
            final String error) {
        if (MIN_VALUE <= l && l <= MAX_VALUE)
            return;

        final StringBuilder message = new StringBuilder();
        if (subject != null) {
            message.append(subject);
            message.append(": ");
        }
        if (error != null) {
            message.append(error);
            message.append(": ");
        }
        message.append(l);
        message.append(" is not within ");
        message.append(MIN_VALUE);
        message.append(" and ");
        message.append(MAX_VALUE);
        message.append(" inclusively.");
        throw new IllegalArgumentException(message.toString());
    }

    /**
     * Checks the parameter range.
     * 
     * @param l The long integer to check to be in the range of an unsigned
     *        integer ({@value SIZE} bits).
     * @throws IllegalArgumentException If {@code l} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static void check(final long l) {
        check(l, "Long integer out of range", null);
    }
}