/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.nio.ByteBuffer;
import javax.annotation.CheckForNull;

/**
 * A secret key for writing and reading protected resources.
 * <p>
 * Implementations do <em>not</em> need to be safe for multi-threading.
 *
 * @param  <K> the type of this secret key.
 * @author Christian Schlichtherle
 */
public interface SecretKey<K extends SecretKey<K>> extends SafeKey<K> {

    /** Returns a protective copy of the secret. */
    @CheckForNull ByteBuffer getSecret();

    /**
     * Clears the current secret and sets it to a protective copy of the given
     * secret.
     *
     * @param secret the secret to copy and set.
     */
    void setSecret(@CheckForNull ByteBuffer secret);

    /**
     * A secret key equals another object if and only if the other object
     * has the same runtime class and its secret property compares equal.
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     * This method is provided for completeness only - you should actually
     * never use secret keys as hash map keys because of their mutable
     * properties!
     */
    @Override
    int hashCode();
}
