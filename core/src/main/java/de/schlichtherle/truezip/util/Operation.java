/*
 * Copyright 2007-2010 Schlichtherle IT Services
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
 * Encapsulates an operation which may throw an exception.
 * This interface may be used to implement closures.
 *
 * @param <E> The type of exception which may be thrown by this operation.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface Operation<E extends Exception> {

    /**
     * Runs this operation.
     *
     * @throws Exception if the operation fails.
     */
    void run() throws E;
}
