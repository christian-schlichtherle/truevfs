/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.ZipEntry.DEFLATED;

/**
 * @author  Christian Schlichtherle
 */
public final class DeflatedZipIT extends ZipTestSuite {
    @Override
    public ZipEntry newEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(DEFLATED);
        return entry;
    }
}