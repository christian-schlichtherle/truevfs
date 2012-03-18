/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes.param.swing;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.pbe.swing.SwingSafePbeParametersView;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A Swing based user interface to prompt for passwords or key files.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class AesCipherParametersView
extends SwingSafePbeParametersView<KeyStrength, AesCipherParameters> {
    @Override
    public AesCipherParameters newPbeParameters() {
        return new AesCipherParameters();
    }
}
