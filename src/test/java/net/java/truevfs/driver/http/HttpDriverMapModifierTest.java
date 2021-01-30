/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.http;

import net.java.truevfs.driver.http.HttpDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class HttpDriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "http|https";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new HttpDriverMapModifier();
    }
}
