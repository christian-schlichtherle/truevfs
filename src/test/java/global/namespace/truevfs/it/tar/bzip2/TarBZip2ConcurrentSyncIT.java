/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.tar.bzip2;

import global.namespace.truevfs.driver.tar.bzip2.TarBZip2Driver;
import global.namespace.truevfs.driver.tar.bzip2.TestTarBZip2Driver;
import global.namespace.truevfs.it.base.ConcurrentSyncITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2ConcurrentSyncIT extends ConcurrentSyncITSuite<TarBZip2Driver> {

    @Override
    protected String getExtensionList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TestTarBZip2Driver();
    }
}
