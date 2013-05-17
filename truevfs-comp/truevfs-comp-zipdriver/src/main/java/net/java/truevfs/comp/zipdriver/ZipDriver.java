/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.nio.charset.Charset;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.zip.DateTimeConverter;
import net.java.truevfs.comp.zip.ZipEntry;

/**
 * An archive driver for ZIP files.
 * By default, ZIP files use the IBM437 character set for the encoding of entry
 * names and comments (unless the General Purpose Bit 11 is present in
 * accordance with appendix D of the
 * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">ZIP File Format Specification</a>).
 * They also apply the date/time conversion rules according to
 * {@link DateTimeConverter#ZIP}.
 * This configuration pretty much constrains the applicability of this driver
 * to North American and Western European countries.
 * However, this driver generally provides best interoperability with third
 * party tools like the Windows Explorer, WinZip, 7-Zip etc.
 * To some extent this applies even outside these countries.
 * Therefore, while you should use this driver to access plain old ZIP files,
 * you should <em>not</em> use it for custom application file formats - use the
 * {@link JarDriver} instead in this case.
 * <p>
 * This driver does <em>not</em> check the CRC value of any entries in existing
 * archives - use {@link CheckedZipDriver} instead.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 *
 * @author Christian Schlichtherle
 */
@Immutable
public class ZipDriver extends AbstractZipDriver<ZipDriverEntry> {

    /**
     * The character set for entry names and comments in &quot;traditional&quot;
     * ZIP files, which is {@code "IBM437"}.
     */
    public static final Charset ZIP_CHARSET = Charset.forName("IBM437");

    /**
     * {@inheritDoc}
     * 
     * @return {@link #ZIP_CHARSET}.
     */
    @Override
    public Charset getCharset() {
        return ZIP_CHARSET;
    }

    @Override
    public ZipDriverEntry newEntry(String name) {
        return new ZipDriverEntry(name);
    }

    @Override
    public ZipDriverEntry newEntry(String name, ZipEntry template) {
        return new ZipDriverEntry(name, template);
    }
}
