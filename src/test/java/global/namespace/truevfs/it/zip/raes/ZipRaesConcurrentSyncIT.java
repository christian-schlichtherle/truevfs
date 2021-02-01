/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip.raes;

import global.namespace.truevfs.driver.zip.raes.TestZipRaesDriver;
import global.namespace.truevfs.it.base.ConcurrentSyncITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesConcurrentSyncIT extends ConcurrentSyncITSuite<TestZipRaesDriver> {

    @Override
    protected String getExtensionList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver();
    }
}
