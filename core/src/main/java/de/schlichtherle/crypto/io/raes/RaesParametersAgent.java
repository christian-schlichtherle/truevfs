/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.crypto.io.raes;

/**
 * These {@link RaesParameters} delegate to some other instance of a sibling
 * interface or itself in order to locate the parameters required to read
 * or write a certain RAES type.
 * This may be implemented by clients to build RAES parameters of a certain
 * type on demand rather than providing them upfront.
 * <p>
 * There are two usages of this interface:
 * <ol>
 * <li>{@link RaesReadOnlyFile#getInstance} uses it to locate RAES parameters
 *     which match the RAES type found in the file unless the provided
 *     parameters already match the required type.
 * <li>{@link RaesOutputStream#getInstance} uses it to allow the client
 *     explict control about the type of RAES file created.
 * </ol>
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public interface RaesParametersAgent extends RaesParameters {
    
    /**
     * Requests an {@link RaesParameters} instance of the given
     * <code>type</code>.
     * It is permissible to return an instance of any other implementation
     * of the <code>RaesParameters</code> interface.
     * If the returned object is an instance of an implementation of this
     * interface, it will be used to continue the search for RAES parameters
     * recursively.
     * Otherwise the search will be aborted.
     *
     * @param type The {@link RaesParameters} interface class which's
     *        implementation is searched.
     *
     * @return An instance of <code>RaesParameters</code> or <code>null</code>
     *         if no RAES parameters are available.
     */
    RaesParameters getParameters(Class type);
}
