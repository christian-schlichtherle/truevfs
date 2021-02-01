/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class SwingAesPbeKeyManagerMapModifierTest extends KeyManagerMapModifierTestSuite {

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new SwingAesPbeKeyManagerMapModifier();
    }
}
