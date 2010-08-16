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

import java.util.zip.Deflater;
import javax.swing.Icon;

/**
 * @deprecated Use {@link ZipDriver} instead.
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class Zip32Driver extends ZipDriver {
    private static final long serialVersionUID = -7061546656075796996L;
    static final String TEMP_FILE_PREFIX = "tzp-zip";
    public static final String DEFAULT_CHARSET = "IBM437";
    public static final int DEFAULT_LEVEL = Deflater.BEST_COMPRESSION;

    public Zip32Driver() {
        this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL);
    }

    public Zip32Driver(String charset) {
        this(charset, null, null, false, false, DEFAULT_LEVEL);
    }

    public Zip32Driver(int level) {
        this(DEFAULT_CHARSET, null, null, false, false, level);
    }

    public Zip32Driver(
            final String charset,
            final boolean preambled,
            final boolean postambled,
            final Icon openIcon,
            final Icon closedIcon) {
        this(charset, openIcon, closedIcon, preambled, postambled, DEFAULT_LEVEL);
    }

    public Zip32Driver(
            final String charset,
            final Icon openIcon,
            final Icon closedIcon,
            final boolean preambled,
            final boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, preambled, postambled, level);
    }
}
