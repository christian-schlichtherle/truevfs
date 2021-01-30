/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.common;

import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.key.spec.prompting.AbstractPromptingPbeParameters;

/**
 * A JavaBean which holds password based encryption parameters for use with the
 * AES cipher.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class AesPbeParameters
extends AbstractPromptingPbeParameters<AesPbeParameters, AesKeyStrength> {

    public AesPbeParameters() { reset(); }

    @Override
    public void reset() {
        super.reset();
        super.setKeyStrength(AesKeyStrength.BITS_128);
    }

    @Override
    public AesKeyStrength[] getAllKeyStrengths() {
        return AesKeyStrength.values();
    }
}
