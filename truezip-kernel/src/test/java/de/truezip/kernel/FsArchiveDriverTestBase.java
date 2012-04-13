/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.TestConfig;
import de.truezip.kernel.FsArchiveDriver;
import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;
import edu.umd.cs.findbugs.annotations.When;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;

/**
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class FsArchiveDriverTestBase<D extends FsArchiveDriver<?>> {

    protected static final boolean ISOLATE_FS_MANAGER = Boolean.getBoolean(
            FsArchiveDriverTestBase.class.getPackage().getName() + ".isolateFsManager");
    static {
        Logger  .getLogger(FsArchiveDriverTestBase.class.getName())
                .log(   Level.CONFIG,
                        "Isolate file system manager: {0}",
                        ISOLATE_FS_MANAGER);
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
        final TestConfig config = TestConfig.push();
        config.setDataSize(data.length * 4 / 3); // account for archive type specific overhead
        config.setIOPoolProvider(null); // reset
    }

    @After
    @OverrideMustInvoke(When.LAST)
    public void tearDown() {
        TestConfig.pop();
    }

    protected final TestConfig getTestConfig() {
        return TestConfig.get();
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
