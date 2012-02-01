/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides constants and static utility methods for unsigned byte integer
 * values ({@value SIZE} bits).
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
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

    /** This class cannot get instantiated. */
    private UByte() {
    }

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
        if (MIN_VALUE <= i && i <= MAX_VALUE)
            return true;

        final StringBuilder message = new StringBuilder();
        if (null != subject) {
            message.append(subject);
            message.append(": ");
        }
        if (null != error) {
            message.append(error);
            message.append(": ");
        }
        message.append(i);
        message.append(" is not within ");
        message.append(MIN_VALUE);
        message.append(" and ");
        message.append(MAX_VALUE);
        message.append(" inclusive.");
        throw new IllegalArgumentException(message.toString());
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
