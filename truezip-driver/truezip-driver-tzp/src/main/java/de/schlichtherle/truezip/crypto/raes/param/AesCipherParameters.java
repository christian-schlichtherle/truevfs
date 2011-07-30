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
package de.schlichtherle.truezip.crypto.raes.param;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.key.pbe.PbeParameters;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

/**
 * A JavaBean which holds AES cipher parameters.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class AesCipherParameters
extends PbeParameters<KeyStrength, AesCipherParameters> {
    private static final Set<KeyStrength>
            AVAILABLE_KEY_STRENGTHS = Collections.unmodifiableSet(
                EnumSet.allOf(KeyStrength.class));

    public AesCipherParameters() {
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        setKeyStrength(KeyStrength.BITS_256);
    }

    @Override
    public Set<KeyStrength> getAvailableKeyStrengths() {
        return AVAILABLE_KEY_STRENGTHS;
    }
}
