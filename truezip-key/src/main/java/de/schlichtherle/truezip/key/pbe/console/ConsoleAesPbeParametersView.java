/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.console;

import de.truezip.key.param.AesKeyStrength;
import de.truezip.key.param.AesPbeParameters;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A console based user interface for prompting for passwords.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ConsoleAesPbeParametersView
extends ConsoleSafePbeParametersView<AesPbeParameters, AesKeyStrength> {

    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}