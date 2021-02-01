/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.mock;

import global.namespace.truevfs.kernel.api.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class MockArchiveDriverTest extends FsArchiveDriverTestSuite<MockArchiveDriverEntry, MockArchiveDriver> {

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
