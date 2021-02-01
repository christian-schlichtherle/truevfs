/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.tar.gzip;

import global.namespace.truevfs.driver.tar.gzip.TarGZipDriver;
import global.namespace.truevfs.driver.tar.gzip.TestTarGZipDriver;
import global.namespace.truevfs.it.base.ConcurrentSyncITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipConcurrentSyncIT extends ConcurrentSyncITSuite<TarGZipDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }
}
