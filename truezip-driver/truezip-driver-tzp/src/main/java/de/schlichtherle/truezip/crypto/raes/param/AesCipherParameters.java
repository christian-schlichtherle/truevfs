/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes.param;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.key.pbe.SafePbeParameters;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A JavaBean which holds AES cipher parameters.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class AesCipherParameters
extends SafePbeParameters<KeyStrength, AesCipherParameters> {

    public AesCipherParameters() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        setKeyStrength(KeyStrength.BITS_256);
    }

    @Override
    public KeyStrength[] getKeyStrengthValues() {
        return KeyStrength.values();
    }
}
