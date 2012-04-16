/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import static de.truezip.driver.zip.io.ZipEntry.BZIP2;

/**
 * @author Christian Schlichtherle
 */
public final class BZip2ZipIT extends ZipTestSuite {
    @Override
    public ZipEntry entry(final String name) {
        final ZipEntry entry = new ZipEntry(name);
        entry.setMethod(BZIP2);
        return entry;
    }
}
