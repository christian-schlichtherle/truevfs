/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.socket.spi.IOPoolService;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TempFilePoolServiceTest {

    private IOPoolService instance;
    
    @Before
    public void setUp() {
        instance = new TempFilePoolService();
    }

    @Test
    public void testGet() {
        assertSame(instance.get(), TempFilePool.INSTANCE);
    }
}
