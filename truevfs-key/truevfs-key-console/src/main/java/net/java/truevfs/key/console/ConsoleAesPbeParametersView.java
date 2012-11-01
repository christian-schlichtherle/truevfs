/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.param.AesKeyStrength;
import net.java.truevfs.key.spec.param.AesPbeParameters;

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
