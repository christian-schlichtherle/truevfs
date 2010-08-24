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
 * Abstract implementation of exception builder.
 * Subclasses must implement {@link #assemble(Throwable, Throwable)} and may
 * override {@link #reset(Throwable)}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @param <C> The type of the cause exception.
 * @param <T> The type of the assembled exception.
 */
public abstract class AbstractExceptionBuilder<C extends Throwable, T extends Throwable>
implements ExceptionBuilder<C, T> {

    private T throwable;

    /**
     * Assembles an exception from the given previous result and a(nother)
     * cause.
     * 
     * @param previous The previous result of calling this method or
     *        {@code null} if this is the first call after object creation or
     *        a call to {@link #reset(Throwable)}.
     * @param cause A(nother) non-{@code null} cause exception to add to the
     *        assembly.
     * @return The assembled exception. {@code null} is not permitted.
     */
    protected abstract T assemble(T previous, C cause);

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} calls
     * <pre>{@code
     * throwable = assemble(throwable, cause);
     * return reset(null);
     * }</pre>
     * where {@code throwable} is the result of any previous assembly or
     * {@code null} if there has been no previous assembly since this exception
     * builder has been instantiated or reset.
     * <p>
     * This enables post-processing the assembled exception by overriding the
     * method {@link #reset(Throwable)} appropriately.
     */
    public final T fail(final C cause) {
        throwable = assemble(throwable, cause);
        return reset(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} calls
     * <pre>{@code
     * throwable = assemble(throwable, cause);
     * }</pre>
     * where {@code throwable} is the result of any previous assembly or
     * {@code null} if there has been no previous assembly since this exception
     * builder has been instantiated or reset.
     */
    public final void warn(final C cause) {
        throwable = assemble(throwable, cause);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} calls
     * <pre>{@code
     * final T t = reset(null);
     * if (t != null)
     *     throw t;
     * }</pre>
     * <p>
     * This enables post-processing the assembled exception by overriding the
     * method {@link #reset(Throwable)} appropriately.
     */
    public final void check() throws T {
        final T t = reset(null);
        if (t != null)
            throw t;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} may get
     * overridden in order to post-process its returned assembled exception
     * like so:
     * <pre>{@code
     * public T reset(final T throwable) {
     *     return post_process(super.reset(throwable));
     * }
     * }</pre>
     */
    public T reset(final T throwable) {
        final T t = this.throwable;
        this.throwable = throwable;
        return t;
    }
}
