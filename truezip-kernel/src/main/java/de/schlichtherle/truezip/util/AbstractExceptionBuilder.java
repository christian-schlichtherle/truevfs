/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Abstract implementation of an exception builder.
 * Subclasses must implement {@link #update(Exception, Exception)} and may
 * override {@link #post(Exception)}.
 *
 * @param  <C> the type of the cause exceptions.
 * @param  <X> the type of the assembled exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractExceptionBuilder< C extends Exception,
                                                X extends Exception>
implements ExceptionBuilder<C, X> {

    private @CheckForNull X assembly;

    /**
     * This method is called to update the given {@code previous} result of
     * the assembly with the given {@code cause}.
     * 
     * @param  cause A(nother) cause exception to add to the assembly.
     * @param  previous The previous result of the assembly or {@code null} if
     *         this is the first call since the creation of this instance or the
     *         last assembly has been checked out.
     * @return The assembled exception. {@code null} is not permitted.
     */
    protected abstract X update(C cause, @CheckForNull X previous);

    /**
     * This method is called to post-process the given result of the assembly
     * after it has been checked out.
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} simply
     * returns the parameter.
     *
     * @param  assembly The checked out result of the exception assembly.
     * @return The post-processed checked out result of the exception assembly.
     */
    protected X post(X assembly) {
        return assembly;
    }

    /**
     * {@inheritDoc}
     *
     * @see #update(Exception, Exception)
     * @see #post(Exception)
     */
    @Override
    public final X fail(C cause) {
        if (null == cause)
            throw new NullPointerException();
        final X assembly = update(cause, this.assembly);
        this.assembly = null;
        return post(assembly);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that the implementation in the class
     * {@link AbstractExceptionBuilder} does <em>not</em> throw an exception.
     *
     * @see #update(Exception, Exception)
     */
    @Override
    public final void warn(C cause) {
        if (null == cause)
            throw new NullPointerException();
        this.assembly = update(cause, this.assembly);
    }

    /**
     * {@inheritDoc}
     *
     * @throws X the assembled exception if the implementation wants
     *         the caller to abort its task.
     * @see    #post(Exception)
     */
    @Override
    public final void check() throws X {
        final X assembly = this.assembly;
        if (null != assembly) {
            this.assembly = null;
            throw post(assembly);
        }
    }
}
