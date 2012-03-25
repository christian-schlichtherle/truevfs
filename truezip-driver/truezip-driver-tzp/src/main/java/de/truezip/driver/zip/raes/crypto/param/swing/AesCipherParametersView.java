/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto.param.swing;

import de.truezip.driver.zip.raes.crypto.param.AesKeyStrength;
import de.truezip.driver.zip.raes.crypto.param.AesCipherParameters;
import de.schlichtherle.truezip.key.pbe.swing.SwingSafePbeParametersView;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class AesCipherParametersView
extends SwingSafePbeParametersView<AesKeyStrength, AesCipherParameters> {
    @Override
    public AesCipherParameters newPbeParameters() {
        return new AesCipherParameters();
    }
}