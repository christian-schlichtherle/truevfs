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

package de.schlichtherle.truezip.io;

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
public class ChainableIOExceptionBuilder<C extends Exception, E extends ChainableIOException>
extends AbstractExceptionBuilder<C, E> {

    private final Class<E> clazz;

    public ChainableIOExceptionBuilder(Class<C> c, Class<E> e) {
        try {
            if (!e.isAssignableFrom(c))
                e.newInstance(); // fail-fast!
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        this.clazz = e;
    }

    /**
     * Links the given exceptions and returns the result. Equivalent to
     * <pre>{@code
     * return (E) cause.initPredecessor(previous);
     * }</pre>
     *
     * @throws IllegalStateException if
     *         {@code cause.}{@link ChainableIOException#getPredecessor()} is
     *         already initialized by a previous call to
     *         {@link ChainableIOException#initPredecessor(ChainableIOException)}.
     */
    @SuppressWarnings("unchecked")
	@Override
    protected final E update(C cause, E previous) {
        final E next;
        try {
            next = clazz.isInstance(cause) ? ((E) cause) : clazz.newInstance();
        } catch (InstantiationException ex) {
            ex.initCause(cause);
            throw new AssertionError(ex);
        } catch (IllegalAccessException ex) {
            ex.initCause(cause);
            throw new AssertionError(ex);
        }
        try {
            return (E) next.initPredecessor(previous);
        } catch (IllegalStateException ex) {
            if (previous != null)
                throw (IllegalStateException) ex.initCause(next);
            return next;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sorts the given exception chain by
     * {@link ChainableIOException#sortPriority() priority}
     * and returns the result.
     */
    @SuppressWarnings("unchecked")
	@Override
    protected final E post(E assembly) {
        return null == assembly ? null : (E) assembly.sortPriority();
    }
}
