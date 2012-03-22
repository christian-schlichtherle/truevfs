/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.test.TestConfig;
import edu.umd.cs.findbugs.annotations.OverrideMustInvoke;
import edu.umd.cs.findbugs.annotations.When;
import java.io.IOException;
import java.util.Random;
import org.junit.After;
import org.junit.Before;

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
