/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.disable;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.key.spec.spi.KeyManagerMapModifier;
import global.namespace.truevfs.comp.key.spec.spi.KeyManagerMapModifierTestSuite;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Schlichtherle
 */
public class DisableAesPbeKeyManagerMapModifierTest extends KeyManagerMapModifierTestSuite {

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new DisableAesPbeKeyManagerMapModifier();
    }

    @Override
    public void testPriority() {
        assertEquals(modifier.getClass().getAnnotation(ServiceImplementation.class).priority(), Integer.MAX_VALUE);
    }
}
