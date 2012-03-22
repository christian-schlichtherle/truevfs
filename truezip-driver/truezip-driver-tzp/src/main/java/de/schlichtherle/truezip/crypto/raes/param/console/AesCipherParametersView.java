/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes.param.console;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.pbe.console.ConsoleSafePbeParametersView;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A console based user interface to prompt for passwords.
 * 
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class AesCipherParametersView
extends ConsoleSafePbeParametersView<KeyStrength, AesCipherParameters> {
    @Override
    public AesCipherParameters newPbeParameters() {
        return new AesCipherParameters();
    }
}