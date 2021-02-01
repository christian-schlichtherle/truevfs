/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zipdriver;

import global.namespace.truevfs.comp.zip.DateTimeConverter;
import global.namespace.truevfs.comp.zip.ZipEntry;

import java.nio.charset.Charset;

/**
 * An archive driver for Java Archive files (JAR).
 * JAR files use the UTF-8 character set for the encoding of entry
 * names and comments.
 * They also apply the date/time conversion rules according to
 * {@link DateTimeConverter#JAR}.
 * This configuration makes this driver applicable for all countries.
 * However, it pretty much constrains the interoperability of this driver to
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
 * @author Christian Schlichtherle
 */
public class JarDriver extends AbstractZipDriver<JarDriverEntry> {

    /**
     * The character set for entry names and comments in JAR files, which is
     * {@code "UTF-8"}.
     */
    public static final Charset JAR_CHARSET = Charset.forName("UTF-8");

    /**
     * {@inheritDoc}
     * 
     * @return {@link #JAR_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return JAR_CHARSET;
    }

    @Override
    public JarDriverEntry newEntry(String name) {
        return new JarDriverEntry(name);
    }

    @Override
    public JarDriverEntry newEntry(String name, ZipEntry template) {
        return new JarDriverEntry(name, template);
    }
}
