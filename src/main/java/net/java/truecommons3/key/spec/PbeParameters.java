/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import net.java.truecommons3.shed.ImplementationsShouldExtend;

import javax.annotation.Nullable;

/**
 * A key with properties for password based encryption (PBE).
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @param  <P> the type of these PBE parameters.
 * @param  <S> the type of the key strength.
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(AbstractPbeParameters.class)
public interface PbeParameters<
        P extends PbeParameters<P, S>,
        S extends KeyStrength>
extends Key<P> {

    /**
     * Returns a protective copy of the password char array.
     * It's highly recommended to overwrite the char array with any
     * non-password data after using the password.
     *
     * @return A protective copy of the password char array.
     */
    @Nullable char[] getPassword();

    /**
     * Copies and stores the given password char array for deriving the cipher
     * key.
     * It's highly recommended to overwrite the char array with any
     * non-password data after calling this method.
     *
     * @param password the password char array for deriving the cipher key.
     */
    void setPassword(@Nullable char[] password);

    /** Returns the cipher key strength. */
    @Nullable S getKeyStrength();

    /**
     * Sets the cipher key strength.
     *
     * @param keyStrength the cipher key strength.
     */
    void setKeyStrength(final @Nullable S keyStrength);

    /**
     * Returns a new non-empty array of all available cipher key strengths.
     * There should be no duplicated elements in this array.
     *
     * @return A new non-empty array of all available cipher key strengths.
     */
    S[] getAllKeyStrengths();
}
