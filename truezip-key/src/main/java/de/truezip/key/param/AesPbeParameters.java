/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.param;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A JavaBean which holds password based encryption parameters for use with the
 * AES cipher.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class AesPbeParameters
extends SafePbeParameters<AesPbeParameters, AesKeyStrength> {

    public AesPbeParameters() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        setKeyStrength(AesKeyStrength.BITS_128);
    }

    @Override
    public AesKeyStrength[] getKeyStrengthValues() {
        return AesKeyStrength.values();
    }
}