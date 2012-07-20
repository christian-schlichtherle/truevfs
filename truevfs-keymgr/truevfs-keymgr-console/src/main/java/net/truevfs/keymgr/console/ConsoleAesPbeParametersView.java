/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.console;

import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.keymgr.spec.param.AesKeyStrength;
import net.truevfs.keymgr.spec.param.AesPbeParameters;

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