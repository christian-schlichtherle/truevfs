/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.driver.mock;

import net.java.truevfs.kernel.spec.FsArchiveDriverTestSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class MockArchiveDriverTest
extends FsArchiveDriverTestSuite<MockArchiveDriverEntry, MockArchiveDriver> {

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
