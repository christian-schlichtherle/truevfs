/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip.it;

import net.java.truevfs.comp.zip.ZipEntry;

import static net.java.truevfs.comp.zip.ZipEntry.BZIP2;

/**
 * @author Christian Schlichtherle
 */
public final class BZip2ZipIT extends ZipITSuite {
    @Override
    public ZipEntry newEntry(final String name) {
        final ZipEntry entry = new ZipEntry(name);
        entry.setMethod(BZIP2);
        return entry;
    }
}
