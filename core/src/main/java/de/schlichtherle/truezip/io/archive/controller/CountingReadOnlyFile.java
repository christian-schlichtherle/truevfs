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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.rof.FilterReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class CountingReadOnlyFile extends FilterReadOnlyFile {
    private static volatile long total;
    private static volatile boolean reset;

    CountingReadOnlyFile(ReadOnlyFile rof) {
        super(rof);
        init();
    }

    /** Returns the total number of bytes read. */
    static long getTotal() {
        return total;
    }

    /**
     * Resets the total number of bytes read if {@link #resetOnInit} has been
     * called before.
     */
    static void init() {
        if (reset) {
            reset = false;
            total = 0;
        }
    }

    /**
     * Requests that the total number of bytes read gets reset on the
     * next call to {@link #init}.
     */
    static void resetOnInit() {
        reset = true;
    }

    @Override
    public int read() throws IOException {
        int ret = rof.read();
        if (ret != -1)
            total++;
        return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int ret = rof.read(b);
        if (ret != -1)
            total += ret;
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = rof.read(b, off, len);
        if (ret != -1)
            total += ret;
        return ret;
    }
}
