/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.def;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.commons.key.api.spi.KeyManagerMapModifier;
import global.namespace.truevfs.commons.key.api.spi.KeyManagerMapModifierTestSuite;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Schlichtherle
 */
public class DefaultAesPbeKeyManagerMapModifierTest extends KeyManagerMapModifierTestSuite {

    @Override
    protected KeyManagerMapModifier newModifier() {
        return new DefaultAesPbeKeyManagerMapModifier();
    }

    @Override
    public void testPriority() {
        assertEquals(modifier.getClass().getAnnotation(ServiceImplementation.class).priority(), Integer.MIN_VALUE);
    }
}
