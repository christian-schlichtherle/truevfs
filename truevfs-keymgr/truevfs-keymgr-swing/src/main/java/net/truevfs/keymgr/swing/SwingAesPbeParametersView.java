/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.swing;

import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.keymgr.spec.param.AesKeyStrength;
import net.truevfs.keymgr.spec.param.AesPbeParameters;

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