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
 * Manages the life cycle of a generic authentication key which is required
 * for the encryption or decryption of protected resources.
 * A key provider is usually associated to one or more protected resources by a
 * {@link KeyManager}.
 * Note that neither the protected resources nor their encryption/decryption
 * operations are modelled by this interface.
 * <p>
 * Clients are assumed to use this interface for the following purposes:
 * <ol>
 * <li>Retrieving an authentication key for encryption of a protected resource
 *     by calling the method {@link #getWriteKey}.
 *     This assumes that the key does not need to get authenticated.
 * <li>Retrieving an authentication key for decryption of a protected resource
 *     by calling the method {@link #getReadKey}.
 *     This assumes that the key needs to get authenticated by another
 *     component which decrypts the protected resource.
 * </ol>
 * The methods of this interface may get executed in arbitrary order.
 * Calling the same method subsequently is guaranteed to return a key which at
 * least compares {@link Object#equals equal}, but is not necessarily the same.
 * <p>
 * Following are some typical use cases:
 * <ol>
 * <li>A new protected resource needs to be created.
 *     In this case, {@link #getWriteKey} needs to get called.
 * <li>The contents of an already existing protected resource need to be
 *     completely replaced.
 *     Hence there is no need to retrieve and authenticate the existing key.
 *     Again, {@link #getWriteKey} needs to get called.
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
 * provider implementation whether or not {@link #getWriteKey} returns a key
 * which compares {@link Object#equals equal} to the key returned by
 * {@link #getReadKey} or returns a completely different key.
 * Ideally, a brave provider implementation would allow the user to control
 * this.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param   <K> The type of the keys.
 * @see     KeyManager
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface KeyProvider<K> {

    /**
     * Returns the authentication key required to encrypt a protected resource.
     * <p>
     * Subsequent calls to this method return an object which at least compares
     * {@link Object#equals equal} to any previously returned object, but is
     * not necessarily the same.
     *
     * @return the key object.
     * @throws UnknownKeyException if the required key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled or
     *         cancelled by the user.
     */
    K getWriteKey() throws UnknownKeyException;

    /**
     * Returns the authentication key required to decrypt a protected resource.
     * This method is expected to be called consecutively until either the
     * returned key is authenticated by another component which decrypts the
     * protected resource or an exception is thrown.
     * <p>
     * Unless {@code invalid} is {@code true}, subsequent calls to this method
     * return an object which at least compares {@link Object#equals equal} to
     * any previously returned object, but is not necessarily the same.
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
     * behaviour in order to protect client applications which do not obey
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
     *        as if prompting for the key had been cancelled.
     */
    void setKey(@CheckForNull K key);
}
