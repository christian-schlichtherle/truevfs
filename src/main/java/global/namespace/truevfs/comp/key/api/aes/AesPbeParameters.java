/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api.aes;

import global.namespace.truevfs.comp.key.api.prompting.AbstractPromptingPbeParameters;

/**
 * A JavaBean which holds password based encryption parameters for use with the AES cipher algorithm.
 *
 * @author Christian Schlichtherle
 */
public final class AesPbeParameters extends AbstractPromptingPbeParameters<AesPbeParameters, AesKeyStrength> {

    public AesPbeParameters() { reset(); }

    @Override
    public void reset() {
        super.reset();
        super.setKeyStrength(AesKeyStrength.BITS_256);
    }

    @Override
    public AesKeyStrength[] getAllKeyStrengths() {
        return AesKeyStrength.values();
    }
}
