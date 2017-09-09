/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.odf;

import net.java.truevfs.driver.odf.OdfDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class OdfDriverMapModifierTest
extends FsDriverMapModifierTestSuite {
    @Override
    protected String getExtensions() {
        return "odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth";
    }

    @Override
    protected FsDriverMapModifier newModifier() {
        return new OdfDriverMapModifier();
    }
}
