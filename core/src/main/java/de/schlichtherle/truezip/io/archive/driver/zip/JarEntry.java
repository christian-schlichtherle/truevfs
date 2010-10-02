/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.zip.DateTimeConverter;

/**
 * Respects the different date/time conversion in JAR files.
 *
 * @see JarDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class JarEntry extends ZipEntry {

    JarEntry(String entryName) {
        super(entryName);
    }

    JarEntry(ZipEntry blueprint) {
        super(blueprint);
    }

    @Override
    protected DateTimeConverter getDateTimeConverter() {
        return DateTimeConverter.JAR;
    }
}
