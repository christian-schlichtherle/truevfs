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

package de.schlichtherle.truezip.io.rof;

import java.io.EOFException;
import java.io.IOException;

/**
 * A base class for {@code ReadOnlyFile} implementations which
 * implements the common boilerplate.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class AbstractReadOnlyFile implements ReadOnlyFile {

    @Override
	public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
	public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
	public void readFully(final byte[] buf, final int off, final int len)
    throws IOException {
        int total = 0, read;
        do {
            read = read(buf, off + total, len - total);
            if (read < 0)
                throw new EOFException();
            total += read;
        } while (total < len);
    }
}
