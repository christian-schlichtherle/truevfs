/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons3.key.spec.common.AesKeyStrength;
import net.java.truecommons3.key.spec.common.AesPbeParameters;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SwingAesPbeParametersView
extends SwingPromptingPbeParametersView<AesPbeParameters, AesKeyStrength> {

    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}
