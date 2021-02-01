/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes.it;

import global.namespace.truevfs.access.it.ConcurrentSyncITSuite;
import global.namespace.truevfs.driver.zip.raes.TestZipRaesDriver;

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
