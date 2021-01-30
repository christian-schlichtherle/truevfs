/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import static java.util.Objects.requireNonNull;

/**
 * Abstract implementation of an exception builder.
 * Subclasses must implement {@link #update(Throwable, Option)} and may
 * override {@link #post(Throwable)}.
 *
 * @param  <I> the type of the input exceptions.
 * @param  <O> the type of the assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
public abstract class AbstractExceptionBuilder< I extends Throwable,
                                                O extends Throwable>
implements ExceptionBuilder<I, O> {

    private Option<O> assembly = Option.none();

    /**
     * {@inheritDoc}
     *
     * @see #update(Throwable, Option)
     * @see #post(Throwable)
     */
    @Override
    public final O fail(I input) {
        final O assembly = update(input);
        this.assembly = Option.none();
        return post(assembly);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} adds
     * the given exception to the assembly for subsequent rethrowing upon a
     * call to {@link #check()}.
     *
     * @see #update(Throwable, Option)
     */
    @Override
    public final void warn(I input) { assembly = Option.some(update(input)); }

    /**
     * {@inheritDoc}
     *
     * @see    #post(Throwable)
     */
    @Override
    public final void check() throws O {
        final Option<O> assembly = this.assembly;
        for (final O t : assembly) {
            this.assembly = Option.none();
            throw post(t);
        }
    }

    private O update(I input) {
        return update(requireNonNull(input), assembly);
    }

    /**
     * Updates the given result of the assembly with the given input exception.
     *
     * @param  input the input exception to handle.
     * @param  assembly the optional previous result of the assembled exception.
     * @return The next assembled (output) exception, never {@code null}.
     */
    protected abstract O update(I input, Option<O> assembly);

    /**
     * This function gets called to post-process the given result of the
     * assembly after it has been checked out.
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} simply
     * returns {@code assembly}.
     *
     * @param  assembly the assembled (output) exception.
     * @return The result of the optional post-processing.
     */
    protected O post(O assembly) { return assembly; }
}
