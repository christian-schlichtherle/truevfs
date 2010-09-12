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

package de.schlichtherle.truezip.io.socket;

/**
 * References a target for I/O operations.
 *
 * @param   <T> The type of the target for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface IOReference<T> {

    /**
     * Returns the target for I/O operations.
     * <p>
     * The returned object reference may be {@code null}.
     * However, this term may be overridden by sub-interfaces or
     * implementations.
     * <p>
     * The result of changing the state of the returned object is undefined.
     * However, this term may be overridden by sub-interfaces or
     * implementations.
     * 
     * @return The target for I/O operations.
     */
    T get();
}
