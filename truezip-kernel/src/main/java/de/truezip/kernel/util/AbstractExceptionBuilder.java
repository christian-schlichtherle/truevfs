/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Abstract implementation of an exception builder.
 * Subclasses must implement {@link #update(Throwable, Throwable)} and may
 * override {@link #post(Throwable)}.
 *
 * @param  <I> the type of the input exceptions.
 * @param  <O> the type of the assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractExceptionBuilder< I extends Throwable,
                                                O extends Throwable>
implements ExceptionBuilder<I, O> {

    private @CheckForNull O assembly;

    /**
     * This funcion gets called to update the given {@code previous} result of
     * the assembly with the given input exception.
     * 
     * @param  input the input exception to handle.
     * @param  assembly the current assembled (output) exception or {@code null}
     *         if this is the first call to this method or the last assembly
     *         has already been checked out.
     * @return The next assembled (output) exception.
     */
    protected abstract O update(I input, @CheckForNull O assembly);

    /**
     * This function gets called to post-process the given result of the
     * assembly after it has been checked out.
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} simply
     * returns the given parameter.
     *
     * @param  assembly the assembled (output) exception.
     * @return The result of the optional post-processing.
     */
    protected O post(O assembly) {
        return assembly;
    }

    /**
     * {@inheritDoc}
     *
     * @see #update(Throwable, Throwable)
     * @see #post(Throwable)
     */
    @Override
    public final O fail(I input) {
        if (null == input)
            throw new NullPointerException();
        final O assembly = update(input, this.assembly);
        this.assembly = null;
        return post(assembly);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that the implementation in the class
     * {@link AbstractExceptionBuilder} does <em>not</em> throw an exception.
     *
     * @see #update(Throwable, Throwable)
     */
    @Override
    public final void warn(I input) {
        if (null == input)
            throw new NullPointerException();
        this.assembly = update(input, this.assembly);
    }

    /**
     * {@inheritDoc}
     *
     * @see    #post(Throwable)
     */
    @Override
    public final void check() throws O {
        final O assembly = this.assembly;
        if (null != assembly) {
            this.assembly = null;
            throw post(assembly);
        }
    }
}
