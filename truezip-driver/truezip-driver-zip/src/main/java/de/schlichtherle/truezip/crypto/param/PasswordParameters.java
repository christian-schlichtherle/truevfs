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
 * A JavaBean for a password.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public interface PasswordParameters {

    /**
     * Returns a protective copy of the password char array.
     *
     * @return A protective copy of the password char array.
     */
    @Nullable char[] getPassword();

    /**
     * Copies the given password char array for deriving the cipher key.
     * This method must make a protective copy of the given password char array.
     * It's highly recommended to overwrite this array with any non-password
     * data after calling this method.
     *
     * @param password the password char array for deriving the cipher key.
     */
    void setPassword(@Nullable char[] password);
}
