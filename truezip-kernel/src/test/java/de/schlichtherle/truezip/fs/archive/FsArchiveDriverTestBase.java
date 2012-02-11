/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import java.io.IOException;
import java.util.Random;
import org.junit.Before;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsArchiveDriverTestBase<D extends FsArchiveDriver<?>> {

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    private static final IOPoolProvider
            IO_POOL_PROVIDER = new ByteArrayIOPoolService(DATA.length * 4 / 3); // account for archive type specific overhead

    private byte[] data;
    private D driver;

    @Before
    public void setUp() throws IOException {
        data = DATA.clone();
        driver = newArchiveDriver(IO_POOL_PROVIDER);
    }

    protected abstract D newArchiveDriver(IOPoolProvider provider);

    protected final D getArchiveDriver() {
        return driver;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected final byte[] getData() {
        return data;
    }

    protected final int getDataLength() {
        return data.length;
    }
}
