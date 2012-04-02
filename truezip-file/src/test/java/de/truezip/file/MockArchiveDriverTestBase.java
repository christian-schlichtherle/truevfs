/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.file;

import de.truezip.kernel.mock.MockArchiveDriver;

/**
 * @author  Christian Schlichtherle
 */
public abstract class MockArchiveDriverTestBase
extends ConfiguredClientTestBase<MockArchiveDriver> {

    @Override
    protected final String getExtensionList() {
        return "mok|mok1|mok2";
    }

    @Override
    protected final MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver(getTestConfig());
    }
}