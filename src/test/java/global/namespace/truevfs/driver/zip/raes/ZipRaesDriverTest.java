/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes;

import global.namespace.truevfs.commons.zipdriver.JarDriverEntry;
import global.namespace.truevfs.it.base.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesDriverTest extends FsArchiveDriverTestSuite<JarDriverEntry, ZipRaesDriver> {

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
