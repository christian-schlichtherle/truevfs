/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.zip;

import de.schlichtherle.truezip.io.util.Paths;
import de.schlichtherle.truezip.util.zip.ZipEntry;

/**
 * A factory for {@link JarEntry}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class JarEntryFactory implements de.schlichtherle.truezip.util.zip.ZipEntryFactory {
    
    public static final JarEntryFactory INSTANCE = new JarEntryFactory();

    private JarEntryFactory() {
    }

    public ZipEntry newZipEntry(String entryName) {
        return new JarEntry(Paths.normalize(entryName, '/'));
    }
}