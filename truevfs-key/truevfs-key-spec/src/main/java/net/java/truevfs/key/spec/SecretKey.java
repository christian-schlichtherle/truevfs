/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.nio.ByteBuffer;
import javax.annotation.CheckForNull;

/**
 * A key with properties for secret key management.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this secret key.
 * @author Christian Schlichtherle
 */
public interface SecretKey<K extends SecretKey<K>> extends Key<K> {

    /** Returns a protective copy of the secret data. */
    @CheckForNull ByteBuffer getSecret();

    /**
     * Clears the current secret data and sets it to a protective copy of the
     * given secret data.
     *
     * @param secret the secret data to copy and set.
     */
    void setSecret(@CheckForNull ByteBuffer secret);
}
