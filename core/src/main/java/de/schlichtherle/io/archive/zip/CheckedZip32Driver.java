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

package de.schlichtherle.io.archive.zip;

import javax.swing.Icon;

/**
 * @deprecated Use {@link CheckedZipDriver} instead.
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.1
 * @see CheckedZip32InputArchive
 */
public class CheckedZip32Driver extends CheckedZipDriver {
    private static final long serialVersionUID = -4645615422084918979L;

    public CheckedZip32Driver() {
        this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL);
    }

    public CheckedZip32Driver(String charset) {
        this(charset, null, null, false, false, DEFAULT_LEVEL);
    }

    public CheckedZip32Driver(int level) {
        this(DEFAULT_CHARSET, null, null, false, false, level);
    }

    public CheckedZip32Driver(
            String charset,
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, preambled, postambled, level);
    }
}
