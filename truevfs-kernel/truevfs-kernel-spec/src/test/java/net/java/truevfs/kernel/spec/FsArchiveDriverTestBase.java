/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;
import edu.umd.cs.findbugs.annotations.When;
import java.io.IOException;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

/**
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class FsArchiveDriverTestBase<D extends FsArchiveDriver<?>> {

    private static final String ISOLATE_FS_MANAGER_PROPERTY_KEY =
            FsManager.class.getName() + ".isolate";
    protected static final boolean ISOLATE_FS_MANAGER =
            Boolean.getBoolean(ISOLATE_FS_MANAGER_PROPERTY_KEY);

    static {
        LoggerFactory
                .getLogger(FsArchiveDriverTestBase.class)
                .debug("Isolate file system manager: {}", ISOLATE_FS_MANAGER);
    }

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    private byte[] data;
    private volatile D driver;

    @Before
    @OverrideMustInvoke(When.FIRST)
    public void setUp() throws IOException {
        data = DATA.clone();
        final FsTestConfig config = FsTestConfig.push();
        config.setDataSize(data.length * 4 / 3); // account for archive type specific overhead
        config.setPool(null); // reset
    }

    @After
    @OverrideMustInvoke(When.LAST)
    public void tearDown() {
        FsTestConfig.pop();
    }

    /*protected final FsTestConfig getTestConfig() {
        return FsTestConfig.get();
    }*/

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
