/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.gzip;

import global.namespace.truevfs.comp.tardriver.TarDriverEntry;
import global.namespace.truevfs.kernel.spec.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipDriverTest extends FsArchiveDriverTestSuite<TarDriverEntry, TarGZipDriver> {

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
