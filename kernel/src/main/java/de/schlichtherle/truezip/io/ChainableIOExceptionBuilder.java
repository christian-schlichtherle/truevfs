/*
 * Copyright 2011 Schlichtherle IT Services
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
import net.jcip.annotations.NotThreadSafe;

/**
 * Assembles chainable I/O exceptions by
 * {@link ChainableIOException#initPredecessor(ChainableIOException) chaining}
 * them.
 * When the assembly is thrown or returned later, it is sorted by
 * {@link ChainableIOException#sortPriority() priority}.
 *
 * @param   <C> The type of the cause exception.
 * @param   <E> The type of the assembled exception.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public class ChainableIOExceptionBuilder<   C extends Exception,
                                            E extends ChainableIOException>
extends AbstractExceptionBuilder<C, E> {

    private final Class<E> clazz;

    /** Static constructor provided for comforting the most prominent use case. */
    public static ChainableIOExceptionBuilder<ChainableIOException, ChainableIOException> newInstance() {
        return new ChainableIOExceptionBuilder<ChainableIOException, ChainableIOException>(ChainableIOException.class, ChainableIOException.class);
    }

    public ChainableIOExceptionBuilder(Class<C> c, Class<E> e) {
        try {
            if (!e.isAssignableFrom(c))
                e.getConstructor(String.class).newInstance("test"); // fail-fast!
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        this.clazz = e;
    }

    /**
     * Chains the given exceptions and returns the result.
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
            next = clazz.isInstance(cause)
                    ? ((E) cause)
                    : clazz.getConstructor(String.class)
                        .newInstance(cause.toString());
        } catch (Exception ex) {
            ex.initCause(cause);
            throw new AssertionError(ex);
        }
        if (next != cause)
            next.initCause(cause);
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
