/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.mock;

import de.truezip.kernel.FsArchiveDriverTestSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class MockArchiveDriverTest
extends FsArchiveDriverTestSuite<MockArchiveDriverEntry, MockArchiveDriver> {

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver(getTestConfig());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}