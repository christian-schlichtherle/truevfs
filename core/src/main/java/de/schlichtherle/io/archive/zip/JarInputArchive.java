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

package de.schlichtherle.io.archive.zip;

import de.schlichtherle.io.archive.spi.InputArchive;
import de.schlichtherle.io.rof.ReadOnlyFile;
import de.schlichtherle.io.util.Paths;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

/**
 * An implementation of {@link InputArchive} to read JAR archives.
 *
 * @see JarDriver
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.7
 */
public class JarInputArchive extends ZipInputArchive {

    public JarInputArchive(
            ReadOnlyFile rof,
            String charset,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, preambled, postambled);
    }

    @Override
    protected de.schlichtherle.util.zip.ZipEntry createZipEntry(String entryName) {
        return new JarEntry(Paths.normalize(entryName, '/'));
    }
}
