/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing;

import net.truevfs.keymanager.swing.SwingPromptingAesPbeKeyManagerMapModifier;
import java.util.Arrays;
import net.truevfs.keymanager.spec.param.AesPbeParameters;
import net.truevfs.keymanager.spec.spi.KeyManagerMapModifier;
import net.truevfs.keymanager.spec.spi.KeyManagerMapModifierTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class SwingPromptingAesPbeKeyManagerMapModifierTest
extends KeyManagerMapModifierTestSuite {
    @Override
    @SuppressWarnings("unchecked")
    protected Iterable<Class<?>> getClasses() {
        return (Iterable<Class<?>>) Arrays.asList((Class<?>) AesPbeParameters.class);
    }

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new SwingPromptingAesPbeKeyManagerMapModifier();
    }
}
