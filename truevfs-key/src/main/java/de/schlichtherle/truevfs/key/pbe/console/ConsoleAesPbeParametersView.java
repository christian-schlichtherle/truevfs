/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.console;

import net.truevfs.key.param.AesKeyStrength;
import net.truevfs.key.param.AesPbeParameters;
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