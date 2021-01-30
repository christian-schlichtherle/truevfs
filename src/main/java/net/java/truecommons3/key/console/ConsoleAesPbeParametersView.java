/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.console;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons3.key.spec.common.AesKeyStrength;
import net.java.truecommons3.key.spec.common.AesPbeParameters;

/**
 * A console based user interface for prompting for passwords.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ConsoleAesPbeParametersView
extends ConsolePromptingPbeParametersView<AesPbeParameters, AesKeyStrength> {

    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}
