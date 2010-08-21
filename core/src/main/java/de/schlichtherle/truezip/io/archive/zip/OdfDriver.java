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

package de.schlichtherle.truezip.io.archive.zip;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;

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
 * Instances of this class are immutable.
 *
 * <h3>How to use this driver</h3>
 * An ODF file is either a plain XML file or a JAR file (called <i>package</i>).
 * <p>
 * If it's an XML file, the method {@link de.schlichtherle.truezip.io.File#isFile}
 * returns {@code true} and this driver is actually never used.
 * It's up to the client application to recognize this and deal with the
 * ODF file appropriately.
 * <p>
 * If it's a JAR file however, the method
 * {@link de.schlichtherle.truezip.io.File#isDirectory} returns {@code true} and
 * this driver is used to access the file.
 * The client application can then access the ODF file transparently as if it
 * were a directory in a path.
 * If a <i>mimetype</i> entry is to be created or modified, this should be
 * done first in order to provide best performance.
 *
 * @see OdfOutputArchive
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class OdfDriver extends JarDriver {
    private static final long serialVersionUID = 1586715698610542033L;

    /**
     * Equivalent to {@link #OdfDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, DEFAULT_LEVEL)}.
     */
    public OdfDriver() {
        this(null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #OdfDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, level)}.
     */
    public OdfDriver(int level) {
        this(null, null, false, false, level);
    }

    /** Constructs a new ODF driver. */
    public OdfDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(openIcon, closedIcon, preambled, postambled, level);
    }

    @Override
    public OutputArchive createOutputArchive(
            Archive archive,
            OutputStream out,
            InputArchive source)
    throws IOException {
        return new OdfOutputArchive(createZipOutputArchive(
                archive, out, (ZipInputArchive) source));
    }
}
