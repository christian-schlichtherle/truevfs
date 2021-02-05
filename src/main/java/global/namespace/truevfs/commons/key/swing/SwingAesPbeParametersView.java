/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.swing;

import global.namespace.truevfs.commons.key.api.common.AesKeyStrength;
import global.namespace.truevfs.commons.key.api.common.AesPbeParameters;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @author Christian Schlichtherle
 */
final class SwingAesPbeParametersView
extends SwingPromptingPbeParametersView<AesPbeParameters, AesKeyStrength> {

    @Override
    public AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }
}
