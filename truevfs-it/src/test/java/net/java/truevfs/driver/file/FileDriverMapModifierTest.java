/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class FileDriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "file";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new FileDriverMapModifier();
    }
}
