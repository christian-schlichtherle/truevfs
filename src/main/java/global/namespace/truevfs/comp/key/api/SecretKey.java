/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import java.nio.ByteBuffer;

/**
 * A key with properties for secret key management.
 * <p>
 * Implementations do not need to be thread-safe.
 *
 * @param  <K> the type of this secret key.
 * @author Christian Schlichtherle
 */
public interface SecretKey<K extends SecretKey<K>> extends Key<K> {

    /** Returns a protective copy of the secret data. */
    ByteBuffer getSecret();

    /**
     * Clears the current secret data and sets it to a protective copy of the
     * given secret data.
     *
     * @param secret the secret data to copy and set.
     */
    void setSecret(ByteBuffer secret);
}
