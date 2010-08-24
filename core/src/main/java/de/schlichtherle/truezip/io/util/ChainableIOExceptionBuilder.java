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

package de.schlichtherle.truezip.io.util;

import de.schlichtherle.truezip.util.AbstractExceptionBuilder;

/**
 * Assembles {@link ChainableIOException}s by
 * {@link ChainableIOException#initPredecessor(ChainableIOException) linking}
 * them.
 * When the assembly is thrown or returned at a later time, it is sorted by
 * {@link ChainableIOException#sortPriority() priority}.
 *
 * @param <E> The type of {@link ChainableIOException} to use for cause and
 *       predecessor exceptions.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ChainableIOExceptionBuilder<E extends ChainableIOException>
extends AbstractExceptionBuilder<E, E> {

    /**
     * Links the given exceptions and returns the result. Equivalent to
     * <pre>{@code
     * return (E) cause.initPredecessor(previous);
     * }</pre>
     *
     * @throws IllegalStateException If {@code cause.}{@link ChainableIOException#getPredecessor()}
     *         is already initialized by a previous call to
     *         {@link ChainableIOException#initPredecessor(ChainableIOException)}.
     */
    protected final E assemble(
            final E cause,
            final E previous) {
        return (E) cause.initPredecessor(previous);
    }

    /**
     * Replaces the assembled exception chain with the given new exception
     * chain.
     * The previously assembled exception chain is sorted by
     * {@link ChainableIOException#sortPriority() priority} before it is
     * returned.
     */
    @Override
    public final E reset(final E exception) {
        final E e = super.reset(exception);
        return e != null ? (E) e.sortPriority() : null;
    }
}
