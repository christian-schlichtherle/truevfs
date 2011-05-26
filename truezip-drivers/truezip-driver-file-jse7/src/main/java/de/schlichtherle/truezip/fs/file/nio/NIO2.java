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
package de.schlichtherle.truezip.fs.file.nio;

import java.nio.file.Files;

/**
 * Holds a static boolean telling us if {@link Files} is
 * available to the JRE.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class NIO2 {

    /**
     * {@code true} if and only if the class {@link Files} is available to
     * the JRE.
     */
    static final boolean AVAILABLE;
    static {
        boolean available;
        try {
            Files.class.getName();
            available = true;
        } catch (NoClassDefFoundError error) {
            available = false;
        }
        AVAILABLE = available;
    }

    private NIO2() {
    }
}
