/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public ZipArchiveEntry newEntry(String name) {
        return new JarArchiveEntry(name);
    }

    @Override
    public ZipArchiveEntry newEntry(String name, ZipEntry template) {
        return new JarArchiveEntry(name, template);
    }
}
