/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import net.java.truevfs.key.spec.safe.SafeKey;
import net.java.truevfs.key.spec.safe.SafeKeyStrength;

/**
 * A prompting key for writing and reading protected resources.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this prompting key.
 * @see    PromptingKeyProvider
 * @author Christian Schlichtherle
 */
public interface PromptingKey<
        K extends PromptingKey<K, S>,
        S extends SafeKeyStrength>
extends SafeKey<K, S> {

    /**
     * Returns a new non-empty array of all available key strength values.
     * There should be no duplicated elements in this array.
     *
     * @return A new non-empty array of all available key strength values.
     */
    public abstract S[] getKeyStrengthValues();

    /**
     * Returns whether or not the user shall get prompted for a new key upon
     * the next call to {@link PromptingKeyProvider#getKeyForWriting},
     * provided that the key has been {@link #setKey set} before.
     *
     * @return Whether or not the user shall get prompted for a new key upon
     *         the next call to {@link PromptingKeyProvider#getKeyForWriting},
     *         provided that the key has been {@link #setKey set} before.
     */
    boolean isChangeRequested();

    /**
     * Requests to prompt the user for a new key upon the next call to
     * {@link PromptingKeyProvider#getKeyForWriting}, provided that the key
     * has been {@link PromptingKeyProvider#setKey set} by then.
     *
     * @param  changeRequested whether or not the user shall get prompted
     *         for a new key upon the next call to
     *         {@link PromptingKeyProvider#getKeyForWriting}, provided that the
     *         key has been {@link PromptingKeyProvider#setKey set} by then.
     */
    void setChangeRequested(boolean changeRequested);
}
