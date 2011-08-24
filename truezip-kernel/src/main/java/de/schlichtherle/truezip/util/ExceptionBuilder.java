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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * An exception builder is an exception handler which assembles an exception
 * of the parameter type {@code T} from one or more exceptions of the parameter
 * type {@code C}.
 * This may be used in scenarios where a cooperative algorithm needs to
 * continue its task even if one or more exceptional conditions occur.
 * This interface would then allow to collect all cause exceptions during
 * the processing by calling {@link #warn(Exception)} and later check out the
 * assembled exception by calling {@link #fail(Exception)} or
 * {@link #check()}.
 *
 * @param   <C> The type of the cause exception.
 * @param   <E> The type of the assembled exception.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public interface ExceptionBuilder<C extends Exception, E extends Exception>
extends ExceptionHandler<C, E> {

    /**
     * Adds the {@code cause} exception to the assembly and
     * checks out and returns
     * the result
     * in order to enable the assembly of another exception.
     * <p>
     * {@inheritDoc}
     *
     * @return The assembled exception to throw.
     */
    @Override
    E fail(C cause);

    /**
     * Adds the {@code cause} exception to the assembly and
     * either returns or checks out and throws
     * the result
     * in order to enable the assembly of another exception.
     * <p>
     * {@inheritDoc}
     *
     * @throws Exception the assembled exception if the implementation wants
     *         the caller to abort its task.
     */
    @Override
    void warn(C cause) throws E;

    /**
     * Either returns or checks out and throws
     * the result of the assembly
     * in order to enable the assembly of another exception.
     *
     * @throws Exception the assembled exception if the implementation wants
     *         the caller to abort its task.
     */
    void check() throws E;
}
