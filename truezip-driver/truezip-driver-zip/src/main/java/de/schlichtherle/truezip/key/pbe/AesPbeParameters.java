/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import net.jcip.annotations.NotThreadSafe;

/**
 * A JavaBean which holds password based encryption parameters for use with the
 * AES cipher.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class AesPbeParameters
extends SafePbeParameters<AesKeyStrength, AesPbeParameters> {

    public AesPbeParameters() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        setKeyStrength(AesKeyStrength.BITS_128);
    }

    @Override
    public AesKeyStrength[] getKeyStrengthValues() {
        return AesKeyStrength.values();
    }
}
