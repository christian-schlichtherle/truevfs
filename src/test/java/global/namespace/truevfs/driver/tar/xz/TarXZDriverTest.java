/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.xz;

import global.namespace.truevfs.commons.tardriver.TarDriverEntry;
import global.namespace.truevfs.it.base.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZDriverTest extends FsArchiveDriverTestSuite<TarDriverEntry, TarXZDriver> {

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
