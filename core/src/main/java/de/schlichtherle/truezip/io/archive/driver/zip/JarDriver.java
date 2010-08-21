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

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import javax.swing.Icon;

/**
 * An archive driver which builds JAR files.
 * JAR files use {@value #DEFAULT_CHARSET} as the character set for
 * entry names and comments.
 * <p>
 * Other than this, JAR files are treated like regular ZIP files.
 * In particular, this class does <em>not</em> check a JAR file for the
 * existance of the <i>META-INF/MANIFEST.MF</i> entry or any other entry.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class JarDriver extends ZipDriver {
    private static final long serialVersionUID = 3333659381918211087L;

    /**
     * The default character set for entry names and comments, which is {@value}.
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * Equivalent to {@link #JarDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, DEFAULT_LEVEL)}.
     */
    public JarDriver() {
        this(null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #JarDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, level)}.
     */
    public JarDriver(int level) {
        this(null, null, false, false, level);
    }

    /** Constructs a new JAR driver. */
    public JarDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(DEFAULT_CHARSET, openIcon, closedIcon, preambled, postambled, level);
    }

    @Override
    protected ZipEntry createZipEntry(ZipEntry template) {
        return new JarEntry(template);
    }

    @Override
    protected ZipEntry createZipEntry(String entryName) {
        return new JarEntry(entryName);
    }

    @Override
    protected ZipInputArchive createZipInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        return new ZipInputArchive(
                rof, getCharset(), JarEntryFactory.INSTANCE,
                getPreambled(), getPostambled());
    }
}
