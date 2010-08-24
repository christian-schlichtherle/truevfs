/*
 * Copyright 2010 Schlichtherle IT Services
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
 * An exception builder is an exception handler which assembles an exception
 * of the parameter type {@code T} from one or more exceptions of the parameter
 * type {@code C}.
 * This may be used in scenarios where a cooperative algorithm needs to
 * continue its task even if one or more exceptional conditions occur.
 * This interface would then allow to collect all cause exceptions during
 * the processing by calling {@link #warn(Throwable)} and later process the
 * assembled exception by calling {@link #fail(Throwable)}, {@link #checkout()}
 * or {@link #reset(Throwable)}.
 *
 * @param <C> The type of the cause exception.
 * @param <T> The type of the assembled exception.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ExceptionBuilder<C extends Throwable, T extends Throwable>
extends ExceptionHandler<C, T> {

    /**
     * {@inheritDoc}
     * <p>
     * If an exception has been assembled from previous calls to the
     * method {@link #warn(Throwable)}, then the {@code cause} exception is
     * added to the assembly and the resulting exception is returned.
     * Finally, the implementation must reset its state so that this instance
     * can be reused to assemble another exception.
     */
    T fail(C cause);

    /**
     * {@inheritDoc}
     * <p>
     * The implementation may store an exception of parameter type {@code T}
     * for deferred processing by the method {@link #fail(Throwable)},
     * {@link #checkout()} or {@link #reset(Throwable)}.
     */
    void warn(C cause) throws T;

    /**
     * Called by a cooperative algorithm in order to check if the
     * implementation has assembled an exception from previous calls to the
     * method {@link #warn(Throwable)}.
     * The implementation may throw an exception parameter type {@code T} or
     * return from the call.
     * Finally, the implementation must reset its state so that this instance
     * can be reused to assemble another exception.
     *
     * @throws T The type of exception to throw, if at all.
     */
    void checkout() throws T;

    /**
     * Resets the assembled exception to the given object and returns the
     * previously assembled exception.
     *
     * @param throwable The new assembled exception - may be {@code null}.
     * @return The old assembled exception - may be {@code null}.
     */
    T reset(final T throwable);
}
