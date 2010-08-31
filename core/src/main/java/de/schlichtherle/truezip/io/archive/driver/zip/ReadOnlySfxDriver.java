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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import static java.util.zip.Deflater.BEST_COMPRESSION;

/**
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them.
 * <p>
 * Instances of this class are immutable.
 * 
 * @see CheckedReadOnlySfxDriver
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @see ReadWriteSfxDriver
 */
public class ReadOnlySfxDriver extends AbstractSfxDriver {
    private static final long serialVersionUID = -993451557140046215L;

    /**
     * Equivalent to {@link #ReadOnlySfxDriver(String, boolean, int)
     * this(DEFAULT_CHARSET, false, Deflater.BEST_COMPRESSION)}.
     */
    public ReadOnlySfxDriver() {
        this(DEFAULT_CHARSET, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ReadOnlySfxDriver(String, boolean, int)
     * this(charset, false, Deflater.BEST_COMPRESSION)}.
     */
    public ReadOnlySfxDriver(String charset) {
        this(charset, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ReadOnlySfxDriver(String, boolean, int)
     * this(DEFAULT_CHARSET, false, level)}.
     */
    public ReadOnlySfxDriver(int level) {
        this(DEFAULT_CHARSET, false, level);
    }

    /** Constructs a new read-only SFX/EXE driver. */
    public ReadOnlySfxDriver(
            String charset,
            boolean postambled,
            final int level) {
        super(charset, postambled, level);
    }

    @Override
    protected ZipOutputArchive newZipOutputArchive(
            Archive archive,
            OutputStream out,
            ZipInputArchive source)
    throws IOException {
        throw new FileNotFoundException(
                "driver class does not support creating or modifying SFX archives");
    }
}
