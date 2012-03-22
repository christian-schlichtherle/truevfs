/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.util.AbstractExceptionBuilder;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Assembles a {@link SequentialIOException} from one or more
 * {@link Exception}s by
 * {@link SequentialIOException#initPredecessor(SequentialIOException) chaining}
 * them.
 * When the assembly is thrown or returned later, it is sorted by
 * {@link SequentialIOException#sortPriority() priority}.
 *
 * @param  <C> the type of the cause exceptions.
 * @param  <X> the type of the assembled exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class SequentialIOExceptionBuilder<  C extends Exception,
                                            X extends SequentialIOException>
extends AbstractExceptionBuilder<C, X> {

    private final Class<X> clazz;

    /**
     * Static constructor provided for comforting the most essential use case.
     * 
     * @return A new sequential I/O exception builder.
     */
    public static SequentialIOExceptionBuilder<Exception, SequentialIOException>
    create() {
        return create(Exception.class, SequentialIOException.class);
    }

    public static <C extends Exception> SequentialIOExceptionBuilder<C, SequentialIOException>
    create(Class<C> clazz) {
        return create(clazz, SequentialIOException.class);
    }

    public static <C extends Exception, X extends SequentialIOException> SequentialIOExceptionBuilder<C, X>
    create(Class<C> cause, Class<X> assembly) {
        return new SequentialIOExceptionBuilder<C, X>(cause, assembly);
    }

    public SequentialIOExceptionBuilder(final Class<C> c, final Class<X> x) {
        try {
            if (!x.isAssignableFrom(c))
                x   .getConstructor(String.class)
                    .newInstance("test")
                    .initCause(null); // fail-fast test!
        } catch (final RuntimeException ex) {
            // E.g. null == c || null == x || ex instanceof SecurityException
            throw ex;
        } catch (final Exception ex) {
            throw new IllegalArgumentException(x.toString(), ex);
        }
        this.clazz = x;
    }

    /**
     * Chains the given exceptions and returns the result.
     *
     * @throws IllegalStateException if
     *         {@code cause.}{@link SequentialIOException#getPredecessor()} is
     *         already initialized by a previous call to
     *         {@link SequentialIOException#initPredecessor(SequentialIOException)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final X update(final C cause, final X previous) {
        final X next;
        try {
            next = (X) (clazz.isInstance(cause)
                    ? cause
                    : clazz .getConstructor(String.class)
                            .newInstance(cause.toString())
                            .initCause(cause));
        } catch (final Exception ex) {
            throw new AssertionError(cause.toString(), ex);
        }
        try {
            return (X) next.initPredecessor(previous);
        } catch (final IllegalStateException ex) {
            if (null != previous)
                throw (IllegalStateException) ex.initCause(next);
            return next;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sorts the given exception chain by
     * {@link SequentialIOException#sortPriority() priority}
     * and returns the result.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final X post(X assembly) {
        return (X) assembly.sortPriority();
    }
}
