/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * Abstract implementation of an exception builder.
 * Subclasses must implement {@link #update(Exception, Exception)} and may
 * override {@link #post(Exception)}.
 *
 * @param   <C> The type of the cause exception.
 * @param   <X> The type of the assembled exception.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class AbstractExceptionBuilder< C extends Exception,
                                                X extends Exception>
implements ExceptionBuilder<C, X> {

    private @CheckForNull X assembly;

    /**
     * This method is called to update the given {@code previous} result of
     * the assembly with the given {@code cause}.
     * 
     * @param  cause A(nother) non-{@code null} cause exception to add to the
     *         assembly.
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
     * @see #post(Exception)
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
