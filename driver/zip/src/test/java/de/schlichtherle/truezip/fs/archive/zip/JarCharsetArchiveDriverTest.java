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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.archive.CharsetArchiveDriverTestCase;
import de.schlichtherle.truezip.socket.MockIOPool;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class JarCharsetArchiveDriverTest
extends CharsetArchiveDriverTestCase {

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver(new MockIOPool());
    }
}
