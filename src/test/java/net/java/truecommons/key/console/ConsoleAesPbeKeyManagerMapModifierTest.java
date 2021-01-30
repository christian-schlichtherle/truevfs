/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.console;

import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifierTestSuite;

import java.util.Collections;

/**
 * @author Christian Schlichtherle
 */
public class ConsoleAesPbeKeyManagerMapModifierTest
extends KeyManagerMapModifierTestSuite {

    @Override
    protected Iterable<Class<?>> getClasses() {
        return Collections.<Class<?>>singleton(AesPbeParameters.class);
    }

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new ConsoleAesPbeKeyManagerMapModifier();
    }
}
