/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.sample;

import java.util.Map;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.prompting.PromptingKeyManager;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;

/** @author Christian Schlichtherle */
//@ServiceImplementation
public class MyKeyManagerMapModifier extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class,
                new PromptingKeyManager<>(new MyPromptingKeyView()));
        return map;
    }
}
