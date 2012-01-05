/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Fixes 32 bit field size in JDK 6 implementation of
 * {@link java.util.zip.Inflater}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class Jdk6Inflater extends Inflater {
    private long read = 0, written = 0;

    Jdk6Inflater(boolean nowrap) {
        super(nowrap);
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        super.setInput(b, off, len);
        read += len;
    }

    @Override
    public int inflate(byte[] b, int off, int len) throws DataFormatException {
        int ilen = super.inflate(b, off, len);
        written += ilen;
        return ilen;
    }

    @Override
    public long getBytesRead() {
        return read - getRemaining();
    }

    @Override
    public long getBytesWritten() {
        return written;
    }

    @Override
    public void reset() {
        super.reset();
        read = written = 0;
    }
}
