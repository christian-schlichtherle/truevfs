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
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * A pool with a single read only file provided to its constructor.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class SingleReadOnlyFilePool implements Pool<ReadOnlyFile, IOException> {
    final ReadOnlyFile rof;

    SingleReadOnlyFilePool(final ReadOnlyFile rof) {
        this.rof = rof;
    }

    @Override
    public ReadOnlyFile allocate() {
        return rof;
    }

    @Override
    public void release(ReadOnlyFile rof) {
        assert this.rof == rof;
    }
}
