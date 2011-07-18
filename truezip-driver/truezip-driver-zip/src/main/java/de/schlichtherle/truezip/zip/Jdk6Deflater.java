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

/**
 * Fixes 32 bit field size in JDK 6 implementation of
 * {@link java.util.zip.Deflater}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class Jdk6Deflater extends ZipDeflater {
    private long read = 0, written = 0;

    @Override
    public void setInput(byte[] b, int off, int len) {
        super.setInput(b, off, len);
        read += len;
    }

    @Override
    public int deflate(byte[] b, int off, int len) {
        int dlen = super.deflate(b, off, len);
        written += dlen;
        return dlen;
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
