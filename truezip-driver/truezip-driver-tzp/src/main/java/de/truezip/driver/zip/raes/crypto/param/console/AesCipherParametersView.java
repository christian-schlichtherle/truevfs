/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto.param.console;

import de.truezip.driver.zip.raes.crypto.param.AesKeyStrength;
import de.truezip.driver.zip.raes.crypto.param.AesCipherParameters;
import de.truezip.kernel.key.pbe.console.ConsoleSafePbeParametersView;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A console based user interface to prompt for passwords.
 * 
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class AesCipherParametersView
extends ConsoleSafePbeParametersView<AesKeyStrength, AesCipherParameters> {
    @Override
    public AesCipherParameters newPbeParameters() {
        return new AesCipherParameters();
    }
}