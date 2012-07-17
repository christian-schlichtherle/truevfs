/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.jar;

import net.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class JarDriverMapModifierTest extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "ear|jar|war";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new JarDriverMapModifier();
    }
}
