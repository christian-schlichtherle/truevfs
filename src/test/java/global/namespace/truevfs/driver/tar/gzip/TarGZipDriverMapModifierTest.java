/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.gzip;

import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifier;
import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarGZipDriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "tar.gz|tar.gzip|tgz";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new TarGZipDriverMapModifier();
    }
}
