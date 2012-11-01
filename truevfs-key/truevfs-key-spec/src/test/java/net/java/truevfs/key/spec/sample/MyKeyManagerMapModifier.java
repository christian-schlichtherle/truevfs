/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.sample;

import java.util.Map;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;

/** @author Christian Schlichtherle */
//@ServiceImplementation
public class MyKeyManagerMapModifier extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class, new MyKeyManager());
        return map;
    }
}
