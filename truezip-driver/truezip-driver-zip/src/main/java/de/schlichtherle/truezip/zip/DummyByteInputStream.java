/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFileInputStream;
import java.io.IOException;

/**
 * A read only file input stream which adds a dummy zero byte to the end of
 * the input in order to support {@link ZipInflaterInputStream}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class DummyByteInputStream extends ReadOnlyFileInputStream {
    private boolean added;

    DummyByteInputStream(ReadOnlyFile rof) {
        super(rof);
    }

    @Override
    public int read() throws IOException {
        final int read = rof.read();
        if (read < 0 && !added) {
            added = true;
            return 0;
        }
        return read;
    }

    @Override
    public int read(final byte[] buf, final int off, int len) throws IOException {
        if (0 == len)
            return 0;
        final int read = rof.read(buf, off, len);
        if (read < len && !added) {
            added = true;
            if (read < 0) {
                buf[0] = 0;
                return 1;
            } else {
                buf[read] = 0;
                return read + 1;
            }
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        int available = super.available();
        return added || available >= Integer.MAX_VALUE ? available : available + 1;
    }
}
