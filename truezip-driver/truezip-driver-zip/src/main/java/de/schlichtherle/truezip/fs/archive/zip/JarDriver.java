/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.zip.DateTimeConverter;
import de.schlichtherle.truezip.zip.ZipEntry;
import java.nio.charset.Charset;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver for Java Archive files (JAR).
 * JAR files use the UTF-8 character set for the encoding of entry
 * names and comments.
 * They also apply the date/time conversion rules according to
 * {@link DateTimeConverter#JAR}.
 * This configuration makes this driver applicable for all countries.
 * However, it pretty much constraints the interoperability of this driver to
 * Java applications and Info-ZIP.
 * Therefore, while you should <em>not</em> use this driver to access plain old
 * ZIP files, you should definitely use it for custom application file formats
 * <p>
 * Other than this, JAR files are treated like plain old ZIP files.
 * In particular, this class does <em>not</em> check or ensure a certain
 * directory structure or the existance of certain entries (e.g.
 * {@code META-INF/MANIFEST.MF}) within a JAR file.
 * <p>
 * This driver does <em>not</em> check the CRC value of any entries in existing
 * archives - use {@link CheckedJarDriver} instead.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
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

    /**
     * Returns a new JAR archive entry with the given {@code name}.
     *
     * @param  name the entry name.
     * @return {@code new JarDriverEntry(name)}
     */
    @Override
    public JarDriverEntry newEntry(String name) {
        return new JarDriverEntry(name);
    }

    /**
     * Returns a new JAR archive entry with the given {@code name} and all
     * other properties copied from the given template.
     *
     * @param  name the entry name.
     * @return {@code new JarDriverEntry(name, template)}
     */
    @Override
    public JarDriverEntry newEntry(String name, ZipEntry template) {
        return new JarDriverEntry(name, template);
    }
}
