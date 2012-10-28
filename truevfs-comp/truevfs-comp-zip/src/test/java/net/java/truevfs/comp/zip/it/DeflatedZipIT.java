/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip.it;

import net.java.truevfs.comp.zip.ZipEntry;
import static net.java.truevfs.comp.zip.ZipEntry.DEFLATED;

/**
 * @author Christian Schlichtherle
 */
public final class DeflatedZipIT extends ZipITSuite {
    @Override
    public ZipEntry newEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(DEFLATED);
        return entry;
    }
}
