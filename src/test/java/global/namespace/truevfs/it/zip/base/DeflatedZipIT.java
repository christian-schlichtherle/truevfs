/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip.base;

import global.namespace.truevfs.comp.zip.ZipEntry;

import static global.namespace.truevfs.comp.zip.ZipEntry.DEFLATED;

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
