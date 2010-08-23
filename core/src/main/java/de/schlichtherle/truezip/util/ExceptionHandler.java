/*
 * Copyright 2007-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.truezip.util;

/**
 * A generic callback interface for cooperational exception handling.
 * In the event of an exceptional condition, an algorithm may call the
 * methods of this interface as the {@code cause} parameter in order to
 * delegate the decision about how to proceed to the implementation.
 * <p>
 * The implementation can then make the following simple decisions:
 * <ol>
 * <li>If possible at all, shall the algorithm proceed?
 * <li>If not, shall the cause be wrapped in another exception?
 * </ol>
 * <p>
 * Optionally, the implementation may perform additional operations such as
 * logging the cause or storing it for later use.
 * This implies that the implementation may be stateful and mutable, which
 * in turn implies that the algorithm should not bypass this interface, i.e.
 * it should never simply create and throw an exception without calling the
 * exception handler.
 * Otherwise some information, such as previous exceptions for example, may get
 * lost.
 * <p>
 * As an example for an algorithm which may benefit from using exception
 * handlers, consider the recursive copying of a directory tree:
 * Among many others, the copy algorithm may encounter the following
 * exceptional conditions:
 * <ul>
 * <li>A source file cannot get opened because of insufficient access
 *     privileges.
 * <li>A destination file cannot get written because the file system is full.
 * </ul>
 * <p>
 * Now the copy algorithm needs to decide whether it shall proceed with the
 * remaining files and directories in the tree and if not, if it should wrap
 * the cause to a different exception type in order to support diagnosing the
 * situation by its client.
 * <p>
 * Ideally, the copy algorithm should use two different exception handlers:
 * One for any exception when reading from the source and another for any
 * exception when writing to the destination.
 * If these exception handlers would map any cause to a different exception
 * type, this would enable the client to analyze the situation and take
 * appropriate action.
 * <p>
 * However, if the client does not want to differentiate this, it could simply
 * provide the same exception handler for both input and output causes to the
 * copy algorithm.
 * <p>
 * Here's how a method declaration for this algorithm could look like:
 * <pre>{@code
 *  public <IE extends Throwable, OE extends Throwable>
 *  copy(   File src, ExceptionHandler<IOException, IE> inputHandler,
 *          File dst, ExceptionHandler<IOException, OE> outputHandler)
 *  throws IE, OE {
 *      // ...
 *  }
 * }</pre>
 * <p>
 * Note that the first parameter type for both handlers is an
 * {@link java.io.IOException} for the cause.
 * The second parameter type determines the type of exception which may be
 * thrown by the handlers themselves and is freely selectable.
 * In general, the parameter type for the cause is determined by the algorithm,
 * whereas the parameter type for the exception is determined by the client.
 * However, for this particular algorithm it is recommended that a client
 * should declare its exception handlers to throw only
 * {@link java.io.IOException}s.
 * Otherwise, the client would be required to catch or declare to throw
 * {@link Throwable}, which is a bad idea in general.
 *
 * @param <C> The type of the cause exception.
 * @param <T> The type of the thrown exception.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ExceptionHandler<C extends Throwable, T extends Throwable> {

    /**
     * Called by an algorithm if an exceptional condition occured which does
     * not allow to proceed the execution.
     * The implementation must return an appropriate exception to be thrown
     * by the algorithm.
     * <p>
     * If the implementation maintains a state, then it must be
     * updated so that this method may be consecutively called by the algorithm
     * without causing the implementation to (re)throw an exception for the
     * same cause unless provided by the algorithm.
     *
     * @param cause The exception to handle.
     *        {@code null} is not permitted.
     * @return The exception to throw.
     *         {@code null} is not permitted.
     */
    T fail(C cause);

    /**
     * Called by an algorithm if an exceptional condition occured which allows
     * to proceed its execution.
     * <p>
     * The implementation may throw an exception parameter type {@code T} or
     * return from the call.
     * Optionally, it may store an exception of parameter type {@code T} for
     * deferred processing by the method {@link #check()}.
     *
     * @param cause The exception to handle.
     *        {@code null} is not permitted.
     * @throws T The type of exception to throw, if at all.
     */
    void warn(C cause) throws T;

    /**
     * Called by an algorithm in order to check if an exception was stored by
     * the method {@link #warn(Throwable)} and process it.
     * The implementation may throw an exception parameter type {@code T} or
     * return from the call.
     * <p>
     * In any case, if the implementation maintains a state, then it must be
     * updated so that this method may be consecutively called by the algorithm
     * without causing the implementation to (re)throw an exception for the
     * same cause unless provided by the algorithm.
     *
     * @throws T The type of exception to throw, if at all.
     */
    void check() throws T;
}
