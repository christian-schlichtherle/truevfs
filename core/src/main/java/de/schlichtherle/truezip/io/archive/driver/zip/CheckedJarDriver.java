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

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import javax.swing.Icon;

/**
 * An archive driver for JAR files which checks the CRC-32 value for all ZIP
 * entries in input archives.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link de.schlichtherle.truezip.io.zip.CRC32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see CheckedZipInputArchive
 */
public class CheckedJarDriver extends JarDriver {
    private static final long serialVersionUID = -2148911260108380591L;

    /**
     * Equivalent to {@link #CheckedJarDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, DEFAULT_LEVEL)}.
     */
    public CheckedJarDriver() {
        this(null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #CheckedJarDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, level)}.
     */
    public CheckedJarDriver(int level) {
        this(null, null, false, false, level);
    }

    /** Constructs a new checked JAR driver. */
    public CheckedJarDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(openIcon, closedIcon, preambled, postambled, level);
    }

    @Override
    protected ZipInputArchive newZipInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new CheckedZipInputArchive(
                rof, getCharset(), JarEntryFactory.INSTANCE,
                getPreambled(), getPostambled());
    }
}
