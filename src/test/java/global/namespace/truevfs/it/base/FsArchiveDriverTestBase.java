/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.base;

import global.namespace.truevfs.kernel.api.FsArchiveDriver;
import global.namespace.truevfs.kernel.api.FsTestConfig;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Random;

/**
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class FsArchiveDriverTestBase<D extends FsArchiveDriver<?>> {

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    private byte[] data;
    private volatile D driver;

    @Before
    public void setUp() throws IOException {
        data = DATA.clone();
        final FsTestConfig config = FsTestConfig.push();
        config.setDataSize(data.length * 4 / 3); // account for archive type specific overhead
        config.setPool(null); // reset
    }

    @After
    public void tearDown() {
        FsTestConfig.pop();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected final byte[] getData() {
        return data;
    }

    protected final int getDataLength() {
        return data.length;
    }

    protected final D getArchiveDriver() {
        final D d = driver;
        return null != d ? d : (driver = newArchiveDriver());
    }

    protected abstract D newArchiveDriver();
}
