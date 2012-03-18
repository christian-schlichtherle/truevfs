/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import javax.annotation.CheckForNull;
import static java.lang.System.arraycopy;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Default implementation for an Extra Field in a Local or Central Header of a
 * ZIP file.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
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
