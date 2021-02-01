/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes;

import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifier;
import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class ZipRaesDriverMapModifierTest
extends FsDriverMapModifierTestSuite {

    @Override
    protected String getExtensions() {
        return "tzp|zip.rae|zip.raes";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new ZipRaesDriverMapModifier();
    }
}
