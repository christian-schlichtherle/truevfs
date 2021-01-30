/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.lang.reflect.InvocationTargetException;

/**
 * Static utility methods for {@link Throwable}s.
 * <p>
 * This class is trivially immutable.
 * 
 * @author Christian Schlichtherle
 */
public class Throwables {

    private Throwables() { }

    /**
     * Wraps the given throwable in a new instance of the same class if
     * possible.
     * 
     * @param  <T> the compile time type of the throwable to return.
     * @param  ex the throwable to wrap in a new instance of the same class.
     * @return a new instance of the same class as the throwable {@code t}
     *         with its {@linkplain Throwable#getCause() cause} initialized to
     *         {@code t} or, if the instantiation fails for some reason,
     *         {@code t} with the failure exception added as a
     *         {@linkplain Throwable#addSuppressed(Throwable) suppressed exception}.
     * @throws SecurityException if getting the constructor fails with this
     *         exception type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T wrap(final T ex) {
        try {
            return (T) ex.getClass()
                        .getConstructor(String.class)
                        .newInstance(ex.toString())
                        .initCause(ex);
        } catch (final NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex2) {
            ex.addSuppressed(ex2);
            return ex;
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