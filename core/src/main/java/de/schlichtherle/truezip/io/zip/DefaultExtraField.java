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

package de.schlichtherle.truezip.io.zip;

/**
 * Default implementation for an Extra Field in a Local or Central Header of a
 * ZIP archive.
 * <p>
 * This class is <em>not</em> thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class DefaultExtraField extends ExtraField {

    private final int headerID;
    private byte[] data;

    /**
     * Creates a new instance of the default Extra Field implementation.
     * 
     * @param headerID An unsigned short integer (two bytes) indicating the
     *        type of the Extra Field.
     * @throws IllegalArgumentException If 
     */
    DefaultExtraField(final int headerID) {
        UShort.check(headerID, "Header ID out of range", null);
        this.headerID = headerID;
    }

    public int getHeaderID() {
        return headerID;
    }

    int getDataSize() {
        return data != null ? data.length : 0;
    }

    void readFrom(final byte[] data, final int off, final int size) {
        UShort.check(size, "Data Size out of range", null);
        this.data = new byte[size];
        System.arraycopy(data, off, this.data, 0, size);
    }

    void writeTo(byte[] data, int off) {
        if (this.data != null)
            System.arraycopy(this.data, 0, data, off, this.data.length);
    }
}