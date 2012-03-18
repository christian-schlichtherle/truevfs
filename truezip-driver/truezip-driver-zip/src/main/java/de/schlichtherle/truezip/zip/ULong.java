/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides constants and static utility methods for unsigned long integer
 * values ({@value SIZE} bits).
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class ULong {

    /**
     * The minimum value of an unsigned long integer,
     * which is {@value MIN_VALUE}.
     */
    public static final long MIN_VALUE = 0;

    /**
     * The maximum value of an unsigned long integer,
     * which is {@value MAX_VALUE}.
     */
    public static final long MAX_VALUE = Long.MAX_VALUE;

    /**
     * The number of bits used to represent an unsigned long integer in
     * binary form, which is {@value SIZE}.
     */
    public static final int SIZE = 63;

    /** This class cannot get instantiated. */
    private ULong() {
    }

    /**
     * Checks the parameter range.
     * 
     * @param  l The long integer to check to be in the range of an unsigned
     *         long integer ({@value SIZE} bits).
     * @param  subject The subject of the exception message
     *         - may be {@code null}.
     *         This should not end with a punctuation character.
     * @param  error First sentence of the exception message
     *         - may be {@code null}.
     *         This should not end with a punctuation character.
     * @return {@code true}
     * @throws IllegalArgumentException If {@code l} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static boolean check(
            final long l,
            final @CheckForNull String subject,
            final @CheckForNull String error) {
        if (MIN_VALUE <= l)
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
        message.append(l);
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
     * @param  l The long integer to check to be in the range of an unsigned
     *         long integer ({@value SIZE} bits).
     * @return {@code true}
     * @throws IllegalArgumentException If {@code l} is less than
     *         {@link #MIN_VALUE} or greater than {@link #MAX_VALUE}.
     */
    public static boolean check(final long l) {
        return check(l, "Long integer out of range", null);
    }
}
