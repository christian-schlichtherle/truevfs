/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import static java.lang.System.*;
import net.jcip.annotations.NotThreadSafe;

/**
 * Default implementation for an Extra Field in a Local or Central Header of a
 * ZIP file.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class DefaultExtraField extends ExtraField {

    private final short headerId;
    private @CheckForNull byte[] data;

    /**
     * Constructs a new Extra Field.
     * 
     * @param  headerId an unsigned short integer (two bytes) indicating the
     *         type of the Extra Field.
     */
    DefaultExtraField(final int headerId) {
        assert UShort.check(headerId);
        this.headerId = (short) headerId;
    }

    @Override
    int getHeaderId() {
        return headerId & UShort.MAX_VALUE;
    }

    @Override
    int getDataSize() {
        final byte[] data = this.data;
        return null != data ? data.length : 0;
    }

    @Override
    void readFrom(final byte[] src, final int off, final int size) {
        assert UShort.check(size);
        arraycopy(src, off, this.data = new byte[size], 0, size);
    }

    @Override
    void writeTo(byte[] dst, int off) {
        final byte[] src = this.data;
        if (null != src)
            arraycopy(src, 0, dst, off, src.length);
    }
}