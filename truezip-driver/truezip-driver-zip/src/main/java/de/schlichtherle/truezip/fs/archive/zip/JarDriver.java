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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.zip.ZipEntry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.nio.charset.Charset;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which builds Java Archive files (JAR).
 * JAR files use UTF-8 as the character set encoding for entry names and
 * comments.
 * <p>
 * Other than this, JAR files are treated like regular ZIP files.
 * In particular, this class does <em>not</em> check a JAR file for the
 * existance of the <i>META-INF/MANIFEST.MF</i> entry or any other entry.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class JarDriver extends ZipDriver {

    /**
     * The character set for entry names and comments in JAR files, which is
     * {@code "UTF-8"}.
     */
    public static final Charset JAR_CHARSET = Charset.forName("UTF-8");

    /**
     * Constructs a new JAR file driver.
     * This constructor uses {@link #JAR_CHARSET} for encoding entry names
     * and comments.
     *
     * @param ioPoolProvider the provider for I/O entry pools for allocating
     *        temporary I/O entries (buffers).
     */
    public JarDriver(IOPoolProvider ioPoolProvider) {
        super(ioPoolProvider, JAR_CHARSET);
    }

    @Override
    public JarArchiveEntry newEntry(String path,
                                    Type type,
                                    Entry template,
                                    BitField<FsOutputOption> mknod)
    throws CharConversionException {
        return (JarArchiveEntry) super.newEntry(path, type, template, mknod);
    }

    @Override
    public JarArchiveEntry newEntry(String name) {
        return new JarArchiveEntry(name);
    }

    @Override
    protected JarArchiveEntry newEntry(String name, ZipEntry template) {
        return new JarArchiveEntry(name, template);
    }
}
