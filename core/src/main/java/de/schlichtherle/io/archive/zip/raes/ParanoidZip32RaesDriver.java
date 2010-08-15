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
 * @deprecated Use {@link ParanoidZipRaesDriver} instead.
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.0
 */
public class ParanoidZip32RaesDriver extends ParanoidZipRaesDriver {
    private static final long serialVersionUID = 6373537182537867796L;

    public ParanoidZip32RaesDriver() {
        this(null, null, false, false, DEFAULT_LEVEL);
    }

    public ParanoidZip32RaesDriver(int level) {
        this(null, null, false, false, level);
    }

    public ParanoidZip32RaesDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(openIcon, closedIcon, preambled, postambled, level);
    }
}
