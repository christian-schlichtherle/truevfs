/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file.zip;

import de.schlichtherle.truezip.fs.archive.zip.OdfDriver;
import de.schlichtherle.truezip.nio.file.TPathITSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class OdfPathIT extends TPathITSuite<OdfDriver> {

    @Override
    protected String getSuffixList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver(getTestConfig().getIOPoolProvider());
    }
}
