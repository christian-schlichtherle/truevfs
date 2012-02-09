/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.socket.IOPool;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsArchiveDriverTestSuite<D extends FsArchiveDriver<?>>
extends FsArchiveDriverTestBase<D> {

    private static final Logger
            logger = Logger.getLogger(FsArchiveDriverTestSuite.class.getName());

    private static final int NUM_ENTRIES = 10;

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Test
    public void testFederated() {
        assert getArchiveDriver().isFederated();
    }

    @Test
    public void testPool() {
        final IOPool<?> p1 = getArchiveDriver().getPool();
        assert null != p1;
        final IOPool<?> p2 = getArchiveDriver().getPool();
        assert null != p2;
        if (p1 != p2)
            logger.log(Level.WARNING, "{0} returns different I/O buffer pools upon multiple invocations of getPool()!", getArchiveDriver().getClass());
    }
}
