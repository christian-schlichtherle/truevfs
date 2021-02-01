/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip;

import global.namespace.truevfs.kernel.spec.spi.FsDriverMapModifier;
import global.namespace.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "zip";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new ZipDriverMapModifier();
    }
}
