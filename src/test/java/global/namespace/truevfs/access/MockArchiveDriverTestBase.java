/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.driver.mock.MockArchiveDriver;

/**
 * @author Christian Schlichtherle
 */
public abstract class MockArchiveDriverTestBase extends ConfiguredClientTestBase<MockArchiveDriver> {

    protected final String getExtensionList() {
        return "mok|mok1|mok2";
    }

    protected final MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver();
    }
}
