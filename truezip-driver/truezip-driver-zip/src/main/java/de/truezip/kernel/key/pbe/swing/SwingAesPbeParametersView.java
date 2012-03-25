/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.pbe.swing;

import de.truezip.kernel.key.param.AesKeyStrength;
import de.truezip.kernel.key.pbe.AesPbeParameters;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class SwingAesPbeParametersView
extends SwingSafePbeParametersView<AesKeyStrength, AesPbeParameters> {
    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}