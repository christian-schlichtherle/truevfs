/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.tar.gzip;

import global.namespace.truevfs.driver.tar.gzip.TarGZipDriver;
import global.namespace.truevfs.driver.tar.gzip.TestTarGZipDriver;
import global.namespace.truevfs.it.tar.base.TarPathITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipPathIT extends TarPathITSuite<TarGZipDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }
}
