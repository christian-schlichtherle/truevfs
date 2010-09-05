/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.impl.zip;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import java.io.IOException;
import java.io.OutputStream;

import static java.util.zip.Deflater.BEST_COMPRESSION;

/**
 * An archive driver which supports building archive files according to
 * the specification of the OpenDocument Format (<i>ODF</i>), version 1.1.
 * This driver ensures that the entry named <i>mimetype</i>, if present at
 * all, is always written as the first entry and uses the STORED method
 * rather than the DEFLATED method in the resulting archive file in order
 * to meet the requirements of section 17.4 of the
 * <a href="http://www.oasis-open.org/committees/download.php/20847/OpenDocument-v1.1-cs1.pdf" target="_blank">OpenDocument Specification</a>,
 * version 1.1.
 * <p>
 * Other than this, ODF files are treated like regular JAR files.
 * In particular, this class does <em>not</em> check an ODF file for the
 * existance of the <i>META-INF/manifest.xml</i> entry or any other entry.
 * <p>
 * When using this driver to create or modify an ODF file, then in order to
 * achieve best performance, the <i>mimetype</i> entry should be created or
 * modified first in order to avoid temp file buffering of any other entries.
 * <p>
 * Instances of this class are immutable.
 *
 * @see OdfOutputArchive
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class OdfDriver extends JarDriver {
    private static final long serialVersionUID = 1586715698610542033L;

    /**
     * Equivalent to {@link #OdfDriver(boolean, boolean, int)
     * this(false, false, Deflater.BEST_COMPRESSION)}.
     */
    public OdfDriver() {
        this(false, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #OdfDriver(boolean, boolean, int)
     * this(false, false, level)}.
     */
    public OdfDriver(int level) {
        this(false, false, level);
    }

    /** Constructs a new ODF driver. */
    public OdfDriver(
            boolean preambled,
            boolean postambled,
            final int level) {
        super(preambled, postambled, level);
    }

    @Override
    public OutputArchive<ZipEntry> newOutputArchive(
            Archive archive,
            OutputStream out,
            ZipInputArchive source)
    throws IOException {
        return new OdfOutputArchive(newZipOutputArchive(
                archive, out, source));
    }
}
