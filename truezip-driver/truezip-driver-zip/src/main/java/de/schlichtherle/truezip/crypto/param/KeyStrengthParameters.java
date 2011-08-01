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
package de.schlichtherle.truezip.crypto.param;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.NotThreadSafe;

/**
 * A JavaBean for a cipher key strength.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public interface KeyStrengthParameters<S extends KeyStrength> {

    /**
     * Returns a new non-empty array of all available key strength values.
     * There should be no duplicated elements in this array.
     *
     * @return A new non-empty array of all available key strength values.
     */
    S[] getKeyStrengthValues();

    /**
     * Returns the cipher key strength.
     *
     * @return The cipher key strength.
     */
    @Nullable S getKeyStrength();

    /**
     * Sets the cipher key strength.
     *
     * @param keyStrength the cipher key strength.
     */
    void setKeyStrength(@Nullable S keyStrength);
}
