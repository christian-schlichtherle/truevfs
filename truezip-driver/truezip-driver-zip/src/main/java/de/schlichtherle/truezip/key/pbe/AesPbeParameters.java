/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.key.pbe;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * A JavaBean which holds password based encryption parameters for use with the
 * AES cipher.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
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
