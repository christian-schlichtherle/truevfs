/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto.param;

import de.schlichtherle.truezip.key.pbe.SafePbeParameters;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A JavaBean which holds AES cipher parameters.
 *
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public final class AesCipherParameters
extends SafePbeParameters<AesKeyStrength, AesCipherParameters> {

    public AesCipherParameters() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        setKeyStrength(AesKeyStrength.BITS_256);
    }

    @Override
    public AesKeyStrength[] getKeyStrengthValues() {
        return AesKeyStrength.values();
    }
}