/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import javax.annotation.CheckForNull;

/**
 * Manages the life cycle of a key for reading and writing a protected
 * resource.
 * A key provider is usually (but not necessarily) associated to one or more
 * protected resources by a {@link KeyManager}.
 * <p>
 * Clients typically use the key for the encryption and authentication of
 * protected resources.
 * However, neither the protected resources nor their encryption or
 * authentication operations are modelled by this interface.
 * Instead, clients are assumed to use it for the following purposes:
 * <ol>
 * <li>The method {@link #prepareWriting} returns the key for writing the
 *     protected resources.
 *     This implies that the key does not need to get validated by any client.
 * <li>The method {@link #prepareReading} returns the key for reading the
 *     protected resources.
 *     This implies that the key needs to get validated by some client.
 * <li>The method {@link #setKey} sets the key programmatically.
 *     This may get used after a call to {@link #prepareReading} in order to
 *     update some properties of the key and implies that the key has been
 *     validated by some client.
 * </ol>
 * The methods of this interface may get executed in arbitrary order.
 * Calling the same method subsequently is guaranteed to return a key which at
 * least compares {@link Object#equals equal}, but is not necessarily the same.
 * <p>
 * Following are some typical use cases:
 * <ol>
 * <li>A new protected resource needs to get created.
 *     In this case, {@link #prepareWriting} needs to get called.
 * <li>The contents of an already existing protected resource need to get
 *     completely replaced.
 *     Hence there is no need to retrieve and validate the key.
 *     Again, {@link #prepareWriting} needs to get called.
 * <li>The contents of an already existing protected resource need to be
 *     read, but not changed.
 *     This implies that the key needs to get retrieved and validated.
 *     In this case, {@link #prepareReading} needs to get called.
 * <li>The contents of an already existing protected resource need to get
 *     read and then only partially updated with new contents.
 *     This implies that the key needs to get retrieved and validated.
 *     Because the contents are only partially updated, changing the key is not
 *     possible.
 *     In this case, just {@link #prepareReading} needs to get called.
 * <li>The contents of an already existing protected resource need to get
 *     read and then entirely replaced with new contents.
 *     This implies that the key needs to get retrieved and validated
 *     before it may optionally get replaced (at the provider's discretion)
 *     with a different key.
 *     In this case, first {@link #prepareReading} and then
 *     {@link #prepareWriting} need to get called.
 * </ol>
 * As you can see in the last example, it is at the discretion of the key
 * provider implementation whether or not {@link #prepareWriting} returns a
 * key which compares {@link Object#equals equal} to the key returned by
 * {@link #prepareReading} or returns a completely different key.
 * Typically, a provider implementation enables the user to control this.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param   <K> The type of the keys.
 * @see     KeyManager
 * @author  Christian Schlichtherle
 */
public interface KeyProvider<K> {

    /**
     * Returns the key for writing a protected resource.
     * This implies that the key does not need to get validated by any client.
     *
     * @return the key for writing a protected resource.
     *         Subsequent calls to this method return a key which at
     *         least compares {@link Object#equals equal} to this key,
     *         but is not necessarily the same.
     * @throws UnknownKeyException if the key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled
     *         or cancelled by the user.
     */
    K prepareWriting() throws UnknownKeyException;

    /**
     * Returns the key for reading a protected resource.
     * This implies that the key needs to get validated by the client.
     * This method is expected to be called consecutively until either the
     * returned key has been validated by another component which actually
     * performs the decryption or an exception is thrown.
     * <p>
     * <b>Important:</b> From a {@code KeyProvider} perspective, a client is
     * not trustworthy!
     * Hence, the implementation should throttle the pace for the return from a
     * subsequent call to this method if the key is invalid in order to protect
     * against an exhaustive search for the correct key.
     * As a rule of thumb, at least three seconds should pass between two
     * consecutive calls to this method by the same thread.
     *
     * @param  invalid {@code true} iff a previous call to this method returned
     *         an invalid key.
     * @return the key for reading a protected resource.
     *         Unless {@code invalid} is {@code true}, subsequent calls to this
     *         method return a key which at least compares
     *         {@link Object#equals equal} to this key, but is not
     *         necessarily the same.
     * @throws UnknownKeyException if the key is unknown for some
     *         reason, e.g. if prompting for the key has been disabled or
     *         cancelled by the user.
     */
    K prepareReading(boolean invalid) throws UnknownKeyException;

    /**
     * Sets the key programmatically.
     * This may get used after a call to {@link #prepareReading} in order to update
     * some properties of the key after it has been validated by the client.
     * <p>
     * Implementations should make a protective copy of the given key in order
     * to protect against subsequent modifications by the client.
     *
     * @param key the key.
     *        If this is {@code null}, this key provider is set to a state
     *        as if prompting for the key had been cancelled.
     */
    void setKey(@CheckForNull K key);
}
