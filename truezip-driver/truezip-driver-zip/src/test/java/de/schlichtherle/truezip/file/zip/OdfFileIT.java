/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TFileTestBase;
import de.schlichtherle.truezip.fs.archive.zip.OdfDriver;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class OdfFileIT extends TFileTestBase<OdfDriver> {

    @Override
    protected String getSuffixList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver(IO_POOL_PROVIDER);
    }
}
