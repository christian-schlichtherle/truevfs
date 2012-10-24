/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.param.AesKeyStrength;
import net.java.truevfs.key.spec.param.AesPbeParameters;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SwingAesPbeParametersView
extends SwingSafePbeParametersView<AesPbeParameters, AesKeyStrength> {
    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}
