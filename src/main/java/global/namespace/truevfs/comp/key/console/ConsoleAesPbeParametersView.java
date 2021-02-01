/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.console;

import global.namespace.truevfs.comp.key.spec.common.AesKeyStrength;
import global.namespace.truevfs.comp.key.spec.common.AesPbeParameters;

/**
 * A console based user interface for prompting for passwords.
 *
 * @author Christian Schlichtherle
 */
final class ConsoleAesPbeParametersView
extends ConsolePromptingPbeParametersView<AesPbeParameters, AesKeyStrength> {

    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}
