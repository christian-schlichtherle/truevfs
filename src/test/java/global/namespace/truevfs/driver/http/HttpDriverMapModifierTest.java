/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.http;

import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifier;
import global.namespace.truevfs.kernel.api.spi.FsDriverMapModifierTestSuite;

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
