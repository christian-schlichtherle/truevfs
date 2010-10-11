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

import java.util.Iterator;

/**
 * An operation container is an operation which runs other operations while
 * using an exception handler to deal with their exceptions.
 *
 * @param <E> The type of exception which may be thrown by this operation.
 * @author Christian Schlichtherle
 * @version $Id$
 * @deprecated This feature is currently unused.
 */
@Deprecated
public class OperationContainer<C extends Exception, E extends Exception>
implements Operation<E> {

    private final Iterable<? extends Operation<? extends C>> operations;
    private final ExceptionHandler<C, E> handler;
    private final Class<C> type;
    private final boolean remove;

    /**
     * @see #run
     */
    public OperationContainer(
            final Iterable<? extends Operation<? extends C>> operationsCollection,
            final boolean removeFromCollectionUponSuccess,
            final ExceptionHandler<C, E> exceptionHandler,
            final Class<C> exceptionType) {
        if (operationsCollection == null || exceptionHandler == null)
            throw new NullPointerException();
        this.operations = operationsCollection;
        this.type = exceptionType;
        this.handler = exceptionHandler;
        this.remove = removeFromCollectionUponSuccess;
    }

    /**
     * Runs the operations in the {@code operationsIterable} parameter
     * provided to the constructor.
     * <p>
     * If an operation returns without an exception and the
     * {@code removeFromCollectionUponSuccess} parameter provided to the
     * constructor is {@code true}, then the operation is removed from the
     * underlying collection.
     * <p>
     * Otherwise, if an operation throws an exception of the
     * {@code exceptionType} parameter provided to the constructor, it's passed
     * to the method {@link ExceptionHandler#warn} of the
     * {@code exceptionHandler} parameter provided to the constructor and
     * execution is continued with the next operation.
     * <p>
     * Otherwise, the exception is cast to a {@link RuntimeException} and
     * rethrown.
     *
     * @throws UnsupportedOperationException if removing a successful operation
     *         from the underlying collection is not supported.
     *         The remaining operations are <em>not<em> executed in this case!
     * @throws RuntimeException if and only if an operation throws a runtime
     *         exception and the exception handler cannot handle this type of
     *         exception.
     *         The remaining operations are <em>not<em> executed in this case!
     * @throws Exception if an operation failed with an exception and the
     *         exception handler throws this exception in return.
     *         The remaining operations are <em>not<em> executed in this case!
     */
    @SuppressWarnings("unchecked")
	@Override
    public void run() throws E {
        final Iterator<? extends Operation<? extends C>> i = operations.iterator();
        while (i.hasNext()) {
            try {
                i.next().run();
            } catch (Exception ex) {
                if (!type.isInstance(ex))
                    throw (RuntimeException) ex;
                handler.warn((C) ex); // call bridge method
                continue;
            }
            if (remove)
                i.remove(); // may throw UnsupportedOperationException
        }
    }
}
