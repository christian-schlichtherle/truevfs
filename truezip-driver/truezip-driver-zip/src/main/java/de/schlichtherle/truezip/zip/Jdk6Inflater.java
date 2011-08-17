/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

    Jdk6Inflater() {
        this(false);
    }

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
        return read;
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
