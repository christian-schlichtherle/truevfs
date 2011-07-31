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
package de.schlichtherle.truezip.crypto;

/**
 * Defines the key strengths for a cipher.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface KeyStrength {

    /** Returns the index of the key strength. */
    int ordinal();

    /** Returns the key strength in bits. */
    int getBits();

    /** Returns the key strength in bytes. */
    int getBytes();

    /**
     * Returns a localized display string representing this key strength.
     */
    @Override
    String toString();
}
