/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
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

import de.schlichtherle.io.archive.Archive;
import de.schlichtherle.io.rof.ReadOnlyFile;
import java.io.IOException;
import javax.swing.Icon;

/**
 * An archive driver for ZIP files which checks the CRC-32 values for all
 * ZIP entries in input archives.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link de.schlichtherle.util.zip.CRC32Exception}.
 * This exception is then propagated through the stack up to the corresponding
 * file operation in the package {@code de.schlichtherle.io} where it is
 * either allowed to pass on or is catched and processed accordingly.
 * For example, the {@link de.schlichtherle.io.FileInputStream#close()}
 * method would allow the {@code CRC32Exception} to pass on to the
 * client application, whereas the
 * {@link de.schlichtherle.io.File#catTo(OutputStream)} method would simply
 * return {@code false}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 * @see CheckedZipInputArchive
 */
public class CheckedZipDriver extends ZipDriver {
    private static final long serialVersionUID = -4645615422084918979L;

    /**
     * Equivalent to {@link #CheckedZipDriver(String, Icon, Icon, boolean, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public CheckedZipDriver() {
        this(DEFAULT_CHARSET, null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #CheckedZipDriver(String, Icon, Icon, boolean, boolean, int)
     * this(charset, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public CheckedZipDriver(String charset) {
        this(charset, null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #CheckedZipDriver(String, Icon, Icon, boolean, boolean, int)
     * this(charset, null, null, false, false, DEFAULT_LEVEL)}.
     */
    public CheckedZipDriver(int level) {
        this(DEFAULT_CHARSET, null, null, false, false, level);
    }

    /** Constructs a new checked ZIP driver. */
    public CheckedZipDriver(
            String charset,
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, preambled, postambled, level);
    }
    
    @Override
    protected ZipInputArchive createZipInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new CheckedZipInputArchive(
                rof, getCharset(), ZipEntryFactory.INSTANCE,
                getPreambled(), getPostambled());
    }
}
