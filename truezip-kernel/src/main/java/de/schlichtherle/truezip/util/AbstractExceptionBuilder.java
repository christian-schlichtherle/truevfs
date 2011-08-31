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

package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * Abstract implementation of an exception builder.
 * Subclasses must implement {@link #update(Exception, Exception)} and may
 * override {@link #post(Exception)}.
 *
 * @param   <C> The type of the cause exception.
 * @param   <E> The type of the assembled exception.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class AbstractExceptionBuilder< C extends Exception,
                                                E extends Exception>
implements ExceptionBuilder<C, E> {

    private @CheckForNull E assembly;

    /**
     * This method is called to update the given {@code previous} result of
     * the assembly with the given {@code cause}.
     * 
     * @param  cause A(nother) non-{@code null} cause exception to add to the
     *         assembly.
     * @param  previous The previous result of the assembly or {@code null} if
     *         this is the first call since the creation of this instance or the
     *         last assembly has been checked out.
     * @return The assembled exception. {@code null} is not permitted.
     */
    protected abstract E update(C cause, @CheckForNull E previous);

    /**
     * This method is called to post-process the given result of the assembly
     * after it has been checked out.
     * <p>
     * The implementation in the class {@link AbstractExceptionBuilder} simply
     * returns the parameter.
     *
     * @param  assembly The checked out result of the exception assembly.
     * @return The post-processed checked out result of the exception assembly.
     */
    protected E post(E assembly) {
        return assembly;
    }

    /**
     * {@inheritDoc}
     *
     * @see #update(Exception, Exception)
     * @see #post(Exception)
     */
    @Override
    public final E fail(C cause) {
        if (null == cause)
            throw new NullPointerException();
        final E assembly = update(cause, this.assembly);
        this.assembly = null;
        return post(assembly);
    }

    /**
     * {@inheritDoc}
     *
     * @see #update(Exception, Exception)
     */
    @Override
    public final void warn(C cause) {
        if (null == cause)
            throw new NullPointerException();
        this.assembly = update(cause, this.assembly);
    }

    /**
     * {@inheritDoc}
     *
     * @see #post(Exception)
     */
    @Override
    public final void check() throws E {
        final E assembly = this.assembly;
        if (null != assembly) {
            this.assembly = null;
            throw post(assembly);
        }
    }
}
