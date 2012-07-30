/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.console;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.keymanager.spec.param.AesKeyStrength;
import net.java.truevfs.keymanager.spec.param.AesPbeParameters;

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