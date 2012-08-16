/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import java.util.Arrays;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifierTestSuite;
import net.java.truevfs.key.console.ConsolePromptingAesPbeKeyManagerMapModifier;

/**
 * @author Christian Schlichtherle
 */
public class ConsolePromptingAesPbeKeyManagerMapModifierTest
extends KeyManagerMapModifierTestSuite {
    @Override
    @SuppressWarnings("unchecked")
    protected Iterable<Class<?>> getClasses() {
        return (Iterable<Class<?>>) Arrays.asList((Class<?>) AesPbeParameters.class);
    }

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new ConsolePromptingAesPbeKeyManagerMapModifier();
    }
}