/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipFileIT extends TFileTestSuite<ZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver(getTestConfig().getIOPoolProvider());
    }
}
