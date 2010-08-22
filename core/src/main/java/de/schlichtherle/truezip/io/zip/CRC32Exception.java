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

package de.schlichtherle.truezip.io.zip;

import java.util.zip.ZipException;

/**
 * Thrown to indicate a CRC-32 mismatch between the declared value in the
 * Central File Header and the Data Descriptor or between the declared value
 * and the computed value from the decompressed data.
 * The prior case is detected on the call to {@link ZipFile#getCheckedInputStream},
 * whereas the latter case is detected when the input stream returned by this
 * method gets closed.
 * <p>
 * The exception's detail message is the name of the ZIP entry.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CRC32Exception extends ZipException {
    private static final long serialVersionUID = 1656298435298526391L;

    final long expectedCrc, actualCrc;

    /**
     * Creates a new instance of {@code CRC32Exception} where the
     * given entry name is the detail message of the base class.
     *
     * @see #getMessage
     * @see #getExpectedCrc
     * @see #getActualCrc
     */
    CRC32Exception(String entryName, long expectedCrc, long actualCrc) {
        super(entryName);
        assert expectedCrc != actualCrc;
        this.expectedCrc = expectedCrc;
        this.actualCrc = actualCrc;
    }

    /**
     * Returns the CRC-32 value which has been read from the ZIP file.
     */
    public long getExpectedCrc() {
        return expectedCrc;
    }

    /**
     * Returns the CRC-32 value which has been computed from the contents
     * of the ZIP entry.
     */
    public long getActualCrc() {
        return actualCrc;
    }
}
