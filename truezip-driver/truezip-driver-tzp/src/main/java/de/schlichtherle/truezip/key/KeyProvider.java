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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A general purpose interface used by client applications to retrieve a
 * key which is required for write or read access to a protected resource.
 * Both the key and the protected resource may be virtually anything:
 * The minimum requirement for a key is just that it's an {@link Object}.
 * The protected resource is not even explicitly modelled in this interface.
 * So in order to use it, an instance must be associated with a protected
 * resource by a third party - this is the job of the {@link KeyManager} class.
 * Because the protected resource is not modelled within this interface,
 * it is at the discretion of the provider implementation whether its
 * instances may or may not be shared among protected resources.
 * If they do, then all associated protected resources share the same key.
 * <p>
 * Once an instance has been associated to a protected resource, the client
 * application is assumed to use the key for two basic operations:
 * <ol>
 * <li>A key is required in order to create a new protected resource or
 *     entirely replace its contents.
 *     This implies that the key does not need to be authenticated.
 *     For this purpose, client applications call the method
 *     {@link #getWriteKey}.
 * <li>A key is required in order to open an already existing protected
 *     resource for access to its contents.
 *     This implies that the key needs to be authenticated by the client
 *     application.
 *     For this purpose, client applications call the method
 *     {@link #getReadKey}.
 * </ol>
 * Calling the same method subsequently is guaranteed to return a key which at
 * least compares {@link Object#equals equal}, but is not necessarily the same.
 * <p>
 * From a client application's perspective, the two basic operations may be
 * executed in no particular order. Following are some typical use cases:
 * <ol>
 * <li>A new protected resource needs to be created.
 *     In this case, just {@link #getWriteKey} needs to get called.
 * <li>The contents of an already existing protected resource need to be
 *     completely replaced.
 *     Hence there is no need to retrieve and authenticate the existing key.
 *     Again, just {@link #getWriteKey} needs to get called.
 * <li>The contents of an already existing protected resource need to be
 *     read, but not changed.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated.
 *     In this case, just {@link #getReadKey} needs to get called.
 * <li>The contents of an already existing protected resource need to be
 *     read and then only partially updated with new contents.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated.
 *     Because the contents are only partially updated, changing the key
 *     is not possible.
 *     Again, just {@link #getReadKey} needs to get called.
 * <li>The contents of an already existing protected resource need to be
 *     read and then entirely replaced with new contents.
 *     This implies that the existing key needs to be retrieved and
 *     authenticated before it is probably (at the provider's discretion)
 *     replaced with a new key.
 *     In this case, first {@link #getReadKey} and then {@link #getWriteKey}
 *     need to get called.
 * </ol>
 * As you can see in the last example, it is at the discretion of the key
 * provider whether or not {@link #getWriteKey} returns a key which compares
 * {@link Object#equals equal} to the key returned by {@link #getReadKey} or
 * returns a completely different new key.
 * Ideally, a brave provider implementation would allow the user to control
 * this.
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
     * Returns the key for (over)writing the contents of a new or existing
     * protected resource.
     * So this key is not going to be used to authenticate an existing resource
     * by the application.
     * <p>
     * Subsequent calls to this method return an object which at least compares
     * {@link Object#equals equal} to the previously returned object, but is
     * not necessarily the same.
     *
     * @return the key object.
     * @throws UnknownKeyException if the required key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled or
     *         cancelled by the user.
     */
    K getWriteKey() throws UnknownKeyException;

    /**
     * Returns the key for reading the contents of an existing protected
     * resource.
     * This method is expected to be called consecutively until either the
     * returned key is verified or an exception is thrown.
     * <p>
     * Unless {@code invalid} is {@code true}, subsequent calls to this method
     * return an object which at least compares {@link Object#equals equal} to
     * the previously returned object, but is not necessarily the same.
     * <p>
     * <b>Important:</b> From an application's perspective, a
     * {@code KeyProvider} is not trustworthy!
     * Hence, the key returned by this method must not only get authenticated,
     * but the application should also throttle the pace for the return from a
     * subsequent call to this method if the key is invalid in order to protect
     * the client application from an exhaustive search for the correct key.
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
    K getReadKey(boolean invalid) throws UnknownKeyException;

    /**
     * Sets the key programmatically.
     *
     * @param key the key.
     *        If this is {@code null}, this key provider is set to a state
     *        as if prompting for the key had been disabled or cancelled.
     */
    void setKey(@CheckForNull K key);
}
