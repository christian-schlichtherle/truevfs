/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An idempotent function.
 * 
 * @see     <a href="http://en.wikipedia.org/wiki/Idempotence>Idempotence</a>
 * @param   <R> The type of the result of the idempotent function.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface IdemPotence<R> {

    /**
     * Maps an object to its idempotent representation.
     * This method is expected to be an idempotent function, i.e. it shall have
     * no side effects and the result of calling the function for its result
     * again at least compares {@link Object#equals} to its initial result.
     * E.g. the method {@link Object#toString} is a prospective implementation
     * of this method because you could call it several times on its result:
     * The first call results in a string and each subsequent call would return
     * the same string again.
     *
     * @param  o the object to map.
     * @return The result of this idempotent function.
     *         May be {@code null} at the discretion of the implementation.
     * @throws NullPointerException at the discretion of the implementation if
     *         {@code o} is {@code null}.
     */
    @Nullable R map(@Nullable Object o);
}
