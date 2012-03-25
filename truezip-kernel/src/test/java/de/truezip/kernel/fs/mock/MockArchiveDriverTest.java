/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.mock;

import de.truezip.kernel.fs.FsCharsetArchiveDriverTestSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class MockArchiveDriverTest
extends FsCharsetArchiveDriverTestSuite<MockArchiveDriverEntry, MockArchiveDriver> {

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver(getTestConfig());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}