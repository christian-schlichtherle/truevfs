/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import org.junit.After;
import org.junit.Before;

import javax.annotation.OverridingMethodsMustInvokeSuper;
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
    @OverridingMethodsMustInvokeSuper
    public void setUp() throws IOException {
        data = DATA.clone();
        final FsTestConfig config = FsTestConfig.push();
        config.setDataSize(data.length * 4 / 3); // account for archive type specific overhead
        config.setPool(null); // reset
    }

    @After
    @OverridingMethodsMustInvokeSuper
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
        final D driver = this.driver;
        return null != driver ? driver : (this.driver = newArchiveDriver());
    }

    protected abstract D newArchiveDriver();
}
