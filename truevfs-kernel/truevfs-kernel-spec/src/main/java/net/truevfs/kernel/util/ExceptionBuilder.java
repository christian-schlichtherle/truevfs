/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * An exception builder is an exception handler which assembles an exception
 * of the parameter type {@code O} from one or more exceptions of the parameter
 * type {@code I}.
 * This may be used in scenarios where a cooperative algorithm needs to
 * continue its task even if one or more input exceptions occur.
 * This interface would then allow to collect all cause exceptions during
 * the processing by calling {@link #warn(Throwable)} and later check out the
 * assembled exception by calling {@link #fail(Throwable)} or
 * {@link #check()}.
 *
 * @param  <I> the type of the input exceptions.
 * @param  <O> the type of the assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public interface ExceptionBuilder<I extends Throwable, O extends Throwable>
extends ExceptionHandler<I, O> {

    /**
     * Adds {@code input} to the assembly and checks out and returns the
     * result in order to enable the assembly of another exception.
     * <p>
     * {@inheritDoc}
     *
     * @return The assembled (output) exception to throw.
     */
    @Override
    O fail(I input);

    /**
     * Adds {@code input} to the assembly and either returns or checks out
     * and throws the result in order to enable the assembly of another output
     * exception.
     * <p>
     * {@inheritDoc}
     *
     * @throws O the assembled (output) exception if the client application
     *         wants the cooperative algorithm to abort its task.
     */
    @Override
    void warn(I input) throws O;

    /**
     * Either returns or checks out and throws the result of the assembly in
     * order to enable the assembly of another output exception.
     *
     * @throws O the assembled (output) exception if the client application
     *         wants the cooperative algorithm to abort its task.
     */
    void check() throws O;
}
