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

import static java.util.zip.Deflater.BEST_COMPRESSION;

/**
 * An archive driver which reads and writes Self Executable (SFX/EXE) ZIP
 * files.
 * <p>
 * <b>Warning:</b> Modifying SFX archives usually voids the SFX code in the
 * preamble!
 * This is because most SFX implementations do not tolerate the contents of
 * the archive to be modified (by intention or accident).
 * When executing the SFX code of a modified archive, anything may happen:
 * The SFX code may be terminating with an error message, crash, silently
 * produce corrupted data, or even something more evil.
 * However, an archive modified with this driver is still a valid ZIP file.
 * So you may still extract the modified archive using a regular ZIP utility.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see ReadOnlySfxDriver
 */
public class ReadWriteSfxDriver extends AbstractSfxDriver {

    private static final long serialVersionUID = -937199842631639717L;

    /**
     * Equivalent to {@link #ReadWriteSfxDriver(String, boolean, int)
     * this(DEFAULT_CHARSET, false, Deflater.BEST_COMPRESSION)}.
     */
    public ReadWriteSfxDriver() {
        this(DEFAULT_CHARSET, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ReadWriteSfxDriver(String, boolean, int)
     * this(charset, false, Deflater.BEST_COMPRESSION)}.
     */
    public ReadWriteSfxDriver(String charset) {
        this(charset, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ReadWriteSfxDriver(String, boolean, int)
     * this(DEFAULT_CHARSET, false, level)}.
     */
    public ReadWriteSfxDriver(int level) {
        this(DEFAULT_CHARSET, false, level);
    }

    /** Constructs a new read-write SFX/EXE driver. */
    public ReadWriteSfxDriver(
            String charset,
            boolean postambled,
            final int level) {
        super(charset, postambled, level);
    }
}
