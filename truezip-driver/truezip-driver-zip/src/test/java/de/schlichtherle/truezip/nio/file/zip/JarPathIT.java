/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file.zip;

import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class JarPathIT extends TPathTestSuite<JarDriver> {

    @Override
    protected String getSuffixList() {
        return "jar";
    }

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver(getTestConfig().getIOPoolProvider());
    }
}
