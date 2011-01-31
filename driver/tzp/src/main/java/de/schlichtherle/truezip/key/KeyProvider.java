/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.key;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A general purpose interface used by client applications to retrieve a
 * key which is required to create or open a protected resource.
 * Both the key and the protected resources may be virtually anything:
 * The minimum requirement for a key is just that it's an {@link Object}.
 * Protected resources are not even explicitly modelled in this interface.
 * So in order to use it, an instance must be associated with a protected
 * resource by a third party - this is the job of the {@link KeyManager} class.
 * Because the protected resource is not modelled within this interface,
 * it is at the discretion of the provider implementation whether its
 * instances may or may not be shared among protected resources.
 * If they do, then all associated protected resources share the same key.
 * <p>
 * For the following examples, it helps if you think of the protected
 * resource being an encrypted file and the key being a password.
 * Of course, this interface also works with certificate based encryption.
 * <p>
 * Once an instance has been associated to a protected resource, the client
 * application is assumed to use the key for two basic operations:
 * <ol>
 * <li>A key is required in order to create a new protected resource or
 *     entirely replace its contents.
 *     This implies that the key does not need to be authenticated.
 *     For this purpose, client applications call the method {@link #getCreateKey}.
 * <li>A key is required in order to open an already existing protected
 *     resource for access to its contents.
 *     This implies that the key needs to be authenticated by the client
 *     application.
 *     For this purpose, client applications call the method {@link #getOpenKey}.
 * </ol>
 * If the same resource is accessed multiple times, these basic operations
 * are guaranteed to return keys which compare {@link Object#equals equal},
 * but is not necessarily the same.
 * <p>
 * From a client application's perspective, the two basic operations may be
 * executed in no particular order. Following are some typical use cases:
 * <ol>
 * <li>A new protected resource needs to be created.
 *     In this case, just the first basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     completely replaced.
 *     Hence there is no need to retrieve and authenticate the existing key.
 *     In this case, just the first basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     read, but not changed.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated.
 *     In this case, just the second basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     read and then only partially updated with new contents.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated.
 *     Because the contents are only partially updated, changing the key
 *     is not possible.
 *     In this case, just the second basic operation is applied.
 * <li>The contents of an already existing protected resource need to be
 *     read and then entirely replaced with new contents.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated before it is probably (at the provider's discretion)
 *     replaced with a new key.
 *     In this case, the second and then the first operation is applied.
 * </ol>
 * As you can see in the last example, it is at the discretion of the key
 * provider whether or not {@link #getCreateKey} returns a key which compares
 * equal to the key returned by {@link #getOpenKey} or returns a completely
 * different (new) key.
 * Ideally, a brave provider implementation would allow the user to control this.
 * In fact, this is the behaviour of the {@link PromptingKeyProvider} in
 * this package and its user interface class(es).
 * <p>
 * Note that provider implementations must be thread-safe.
 * This allows clients to use the same provider by multiple threads
 * concurrently.
 *
 * @param   <K> The type of the keys.
 * @see     KeyManager
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface KeyProvider<K> {

    /** A factory for key providers. */
    public interface Factory<K, P extends KeyProvider<K>> {

        /**
         * Returns a new key provider.
         *
         * @return a new key provider.
         */
        P newKeyProvider();
    } // interface Factory

    /**
     * Returns the key which should be used to create a new protected
     * resource or entirely replace the contents of an already existing
     * protected resource.
     * Hence, this key is not going to be used to authenticate an existing
     * resource by the client application.
     * <p>
     * Each call to this method returns an object which compares
     * {@link Object#equals equal} to the previously returned object,
     * but is not necessarily the same.
     *
     * @return the key object.
     * @throws UnknownKeyException if the required key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled or
     *         cancelled by the user.
     */
    K getCreateKey() throws UnknownKeyException;

    /**
     * Returns the key which should be used to open an existing protected
     * resource in order to access its contents.
     * This method is expected to be called consecutively until either the
     * returned key is verified or an exception is thrown.
     * <p>
     * Unless {@code invalid} is {@code true}, each call to this method returns
     * an object which compares {@link Object#equals equal} to the previously
     * returned object,
     * but is not necessarily the same.
     * <p>
     * <b>Important:</b> From a client application's perspective, a
     * {@code KeyProvider} is not trustworthy!
     * Hence, the key returned by this method must not only get authenticated,
     * but the client application should also throttle the pace for the
     * return from a subsequent call to this method if the key is invalid
     * in order to protect the client application from an exhaustive search
     * for the correct key.
     * As a rule of thumb, at least three seconds should pass between two
     * consecutive calls to this method by the same thread.
     * "Safe" implementations of this interface should enforce this
     * behaviour in order to protect client applications which do not obeye
     * these considerations against abuses of the key provider implementation.
     *
     * @param  invalid {@code true} iff a previous call to this method resulted
     *         in an invalid key.
     * @return the key object.
     * @throws UnknownKeyException if the required key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled or
     *         cancelled by the user.
     */
    K getOpenKey(boolean invalid) throws UnknownKeyException;
}
