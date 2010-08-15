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

package de.schlichtherle.io.archive.zip.raes;

/**
 * An unsafe archive driver which builds RAES encrypted ZIP files.
 * This class only authenticates the cipher key and file length, which makes
 * it comparably fast.
 * However, it does <em>not</em> check the cipher text of input archives and
 * does <em>not</em> check the CRC-32 values of the encrypted archive entries,
 * so it's completely unsafe.
 * <p>
 * Instances of this class are immutable.
 * 
 * @deprecated This class exists for testing purposes only.
 *             Client applications should use {@link SafeZipRaesDriver}
 *             instead.
 * @see SafeZipRaesDriver
 * @see ParanoidZipRaesDriver
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public class UnsafeZipRaesDriver extends AbstractZipRaesDriver {
    private static final long serialVersionUID = 782920123547630730L;

    public UnsafeZipRaesDriver() {
        super(null, null, false, false, DEFAULT_LEVEL, -1L);
    }
}
