/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.file.TFileITSuite;
import de.truezip.driver.zip.OdfDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class OdfFileIT extends TFileITSuite<OdfDriver> {

    @Override
    protected String getSuffixList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver(getTestConfig().getIOPoolProvider());
    }
}