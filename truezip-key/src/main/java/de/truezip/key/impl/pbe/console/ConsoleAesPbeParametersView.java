/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.impl.pbe.console;

import de.truezip.key.param.AesKeyStrength;
import de.truezip.key.param.AesPbeParameters;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A console based user interface to prompt for passwords.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class ConsoleAesPbeParametersView
extends ConsoleSafePbeParametersView<AesPbeParameters, AesKeyStrength> {
    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}