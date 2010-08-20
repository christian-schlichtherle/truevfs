/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.key;

/**
 * A {@link KeyProvider} which allows to select the cipher key strength
 * when creating a new AES encrypted resource or replacing the entire
 * contents of an already existing AES encrypted resource.
 * The cipher key strength for the AES encryption may be either 128, 192 or
 * 256 bits.
 * <p>
 * Note that provider implementations must be thread safe.
 * This allows clients to use the same provider by multiple threads
 * concurrently.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 */
public interface AesKeyProvider<K extends Cloneable> extends KeyProvider<K> {

    /** Identifier for a 128 bit ciphering key. */
    int KEY_STRENGTH_128 = 0;

    /** Identifier for a 192 bit ciphering key. */
    int KEY_STRENGTH_192 = 1;

    /** Identifier for a 256 bit ciphering key. */
    int KEY_STRENGTH_256 = 2;

    /**
     * Returns the cipher key strength for the AES encryption.
     *
     * @return One of {@code KEY_STRENGTH_128},
     *        {@code KEY_STRENGTH_192} or {@code KEY_STRENGTH_256}.
     */
    int getKeyStrength();

    /**
     * Sets the cipher key strength for the AES encryption.
     *
     * @param keyStrength One of {@code KEY_STRENGTH_128},
     *        {@code KEY_STRENGTH_192} or {@code KEY_STRENGTH_256}.
     * @throws IllegalArgumentException If the preconditions for the parameter
     *         do not hold.
     */
    void setKeyStrength(int keyStrength);
}
