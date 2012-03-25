/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import javax.annotation.concurrent.Immutable;

/**
 * Static utility methods for {@link Throwable}s.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class Throwables {

    /* Can't touch this - hammer time! */
    private Throwables() { }

    /**
     * Wraps the given throwable in a new instance of the same class.
     * 
     * @param  <T> the compile time type of the throwable to return.
     * @param  t the throwable to wrap in a new instance of the same class.
     * @return a new instance of the same class as the throwable {@code t}
     *         with its {@linkplain Throwable#getCause() cause} initialized to
     *         {@code t}.
     * @throws IllegalArgumentException If the class of the throwable {@code t}
     *         does not have a public constructor with a single string
     *         parameter or the cause of the new instance cannot get
     *         {@linkplain Throwable#initCause initialized} to {@code t}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T wrap(final T t) {
        try {
            return (T) t.getClass()
                        .getConstructor(String.class)
                        .newInstance(t.toString())
                        .initCause(t);
        } catch (final RuntimeException ex) {
            // E.g. null == t || ex instanceof SecurityException
            throw ex;
        } catch (final Exception ex) {
            throw new IllegalArgumentException(t.toString(), ex);
        }
    }

    /**
     * Returns {@code true} if and only if {@code thiz} is identical to
     * {@code that} or has been (recursively)
     * {@linkplain Throwable#initCause caused} by it.
     * 
     * @param  thiz the throwable to search for {@code that} throwable.
     * @param  that the throwable to look up.
     * @return {@code true} if and only if {@code thiz} is identical to
     *         {@code that} or has been (recursively) caused by it.
     */
    public static boolean contains(Throwable thiz, final Throwable that) {
        do {
            if (thiz == that)
                return true;
        } while (null != (thiz = thiz.getCause()));
        return false;
    }
}