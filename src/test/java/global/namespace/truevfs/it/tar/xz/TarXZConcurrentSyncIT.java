/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.tar.xz;

import global.namespace.truevfs.driver.tar.xz.TarXZDriver;
import global.namespace.truevfs.driver.tar.xz.TestTarXZDriver;
import global.namespace.truevfs.it.base.ConcurrentSyncITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZConcurrentSyncIT extends ConcurrentSyncITSuite<TarXZDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.xz";
    }

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }
}
