/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.disable;

import net.java.truecommons3.key.spec.common.AesPbeParameters;
import net.java.truecommons3.key.spec.spi.KeyManagerMapModifier;
import net.java.truecommons3.key.spec.spi.KeyManagerMapModifierTestSuite;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

/**
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public class DisableAesPbeKeyManagerMapModifierTest
extends KeyManagerMapModifierTestSuite {

    @Override
    protected Iterable<Class<?>> getClasses() {
        return Collections.<Class<?>>singleton(AesPbeParameters.class);
    }

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new DisableAesPbeKeyManagerMapModifier();
    }

    @Override
    public void testPriority() {
        assertTrue(modifier.getPriority() == Integer.MAX_VALUE);
    }
}
