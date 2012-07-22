/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.comp.zip;

import static net.truevfs.comp.zip.ZipEntry.DEFLATED;

/**
 * @author Christian Schlichtherle
 */
public final class DeflatedZipIT extends ZipTestSuite {
    @Override
    public ZipEntry newEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(DEFLATED);
        return entry;
    }
}
