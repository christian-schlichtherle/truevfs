/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.kernel.api.mock.MockArchiveDriver;

/**
 * @author Christian Schlichtherle
 */
public abstract class MockArchiveDriverTestBase
extends ConfiguredClientTestBase<MockArchiveDriver> {

    @Override
    protected final String getExtensionList() {
        return "mok|mok1|mok2";
    }

    @Override
    protected final MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver();
    }
}
