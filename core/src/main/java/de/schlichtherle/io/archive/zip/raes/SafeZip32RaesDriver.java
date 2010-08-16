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

import javax.swing.Icon;

/**
 * @deprecated Use {@link SafeZipRaesDriver} instead.
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class SafeZip32RaesDriver extends SafeZipRaesDriver {
    private static final long serialVersionUID = 7701862504670759996L;
    public static final long DEFAULT_AUTHENTICATION_TRIGGER = 512 * 1024;

    public SafeZip32RaesDriver() {
        this(null, null, false, false, DEFAULT_LEVEL, DEFAULT_AUTHENTICATION_TRIGGER);
    }

    public SafeZip32RaesDriver(int level) {
        this(null, null, false, false, level, DEFAULT_AUTHENTICATION_TRIGGER);
    }

    public SafeZip32RaesDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level,
            final long authenticationTrigger) {
        super(openIcon, closedIcon, preambled, postambled, level, authenticationTrigger);
    }
}
