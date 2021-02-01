/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar;

import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifier;
import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarDriverMapModifierTest
extends FsDriverMapModifierTestSuite {

    @Override
    protected String getExtensions() {
        return "tar";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new TarDriverMapModifier();
    }
}
