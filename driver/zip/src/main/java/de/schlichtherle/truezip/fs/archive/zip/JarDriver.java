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

package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.Entry;
import java.io.CharConversionException;
import java.nio.charset.Charset;

import static java.util.zip.Deflater.BEST_COMPRESSION;

/**
 * An archive driver which builds JAR files.
 * JAR files use {@code "UTF-8"} as the character set for entry names and
 * comments.
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
     * The default character set for entry names and comments, which is
     * {@code "UTF-8"}.
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final Charset JAR_CHARSET = Charset.forName("UTF-8");

    /**
     * Equivalent to {@link #JarDriver(boolean, boolean, int)
     * this(null, null, false, false, Deflater.BEST_COMPRESSION)}.
     */
    public JarDriver() {
        this(false, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #JarDriver(boolean, boolean, int)
     * this(false, false, level)}.
     */
    public JarDriver(int level) {
        this(false, false, level);
    }

    /** Constructs a new JAR driver. */
    public JarDriver(boolean preambled, boolean postambled, final int level) {
        super(JAR_CHARSET, preambled, postambled, level);
    }

    @Override
    public JarEntry newEntry(String path, Type type, Entry template)
    throws CharConversionException {
        return (JarEntry) super.newEntry(path, type, template);
    }

    @Override
    public JarEntry newEntry(String name) {
        return new JarEntry(name);
    }

    @Override
    public JarEntry newEntry(String name, ZipEntry template) {
        return new JarEntry(name, template);
    }
}
