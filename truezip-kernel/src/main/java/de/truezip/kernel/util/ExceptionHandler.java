/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A generic callback interface designed to be implemented by client
 * applications in order to inject an exception handling strategy into a
 * cooperative algorithm as its dependency.
 * In the event of an exception, the cooperative algorithm would then call this
 * interface with an input exception in order to let the implementation decide
 * how to proceed.
 * The implementation can then make the following decisions:
 * <ol>
 * <li>If possible, shall the control be returned to the cooperative algorithm
 *     in order to proceed its task?
 * <li>If not, shall the input exception get mapped to or composed into an
 *     output exception of the same or another type before throwing it?
 * </ol>
 * <p>
 * Optionally, the implementation may perform additional operations such as
 * logging the input exception or storing it for later use.
 * This implies that the implementation may be stateful and mutable, which
 * in turn implies that the cooperative algorithm should not bypass this
 * interface, i.e. it should never simply create and throw an exception without
 * calling this exception handler.
 * Otherwise some information, such as previous cause exceptions for example,
 * could get lost.
 * <p>
 * In general, the type parameter for the input exception is determined by the
 * cooperative algorithm, whereas the type parameter for the output exception
 * is determined by the client application.
 * Where possible, the cooperative algorithm should declare generic method
 * signatures in order to enable the client application to select the type of
 * the thrown exception as desired (see below).
 * <p>
 * As an example for a cooperative algorithm which may benefit from using
 * this interface, consider the recursive copying of a directory tree:
 * Among many others, the copy algorithm may encounter the following
 * exceptional conditions:
 * <ul>
 * <li>A source file cannot get opened because of insufficient access
 *     privileges.
 * <li>A destination file cannot get written because the file system is full.
 * </ul>
 * <p>
 * Now the implementation may decide whether the copy algorithm shall proceed
 * with the remaining files and directories in the tree or if not, if the cause
 * exception should get wrapped in another exception in order to enable
 * diagnosing the situation by its client application.
 * <p>
 * The copy algorithm could use two different exception handlers:
 * One for any exception when reading from a source file and another one for
 * any exception when writing to a destination file.
 * If these exception handlers would map any cause to a different exception
 * type, then this would enable the client application to analyze the situation
 * and take appropriate action.
 * <p>
 * However, if the client application doesn't care, it could simply provide the
 * same exception handler for both input and output exceptions to the copy
 * algorithm.
 * <p>
 * Here's how a generic method declaration for a copy algorithm could look like:
 * <pre>{@code
 * public <I extends Exception, O extends Exception>
 * copy(   File src, ExceptionHandler<IOException, I> inputHandler,
 *         File dst, ExceptionHandler<IOException, O> outputHandler)
 * throws I, O {
 *     // ...
 * }
 * }</pre>
 * <p>
 * Note that the first type parameter for both handlers is an
 * {@link java.io.IOException} for the input exceptions whereas the the second
 * type parameter determines the type of the output exceptions which may get
 * thrown by the exception handlers themselves.
 * 
 * @param  <I> the type of the input exceptions.
 * @param  <O> the type of the output exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public interface ExceptionHandler<I extends Throwable, O extends Throwable> {

    /**
     * Called to handle an exception which <em>doesn't</em> allow the caller
     * to proceed with its task.
     * The implementation must return an appropriate exception to be thrown
     * by the cooperative algorithm.
     * Finally, if the implementation maintains a state, it must get updated
     * so that this instance can get reused to handle more exceptions!
     *
     * @param  input the input exception to handle.
     * @return The output exception to throw.
     */
    O fail(I input);

    /**
     * Called to handle an exception which <em>may</em> allow the caller to
     * proceed with its task.
     * The implementation may return from the call or throw an exception of the
     * parameter type {@code O}.
     * If the implementation maintains a state, it must get updated
     * so that this instance can get reused to handle more exceptions.
     *
     * @param  input the input exception to handle.
     * @throws O the output exception if the client application
     *         wants the cooperative algorithm to abort its task.
     */
    void warn(I input) throws O;
}
