/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.spec.sample;

import global.namespace.truevfs.comp.key.spec.KeyManager;
import global.namespace.truevfs.comp.key.spec.common.AesPbeParameters;
import global.namespace.truevfs.comp.key.spec.prompting.PromptingKeyManager;
import global.namespace.truevfs.comp.key.spec.spi.KeyManagerMapModifier;

import java.util.Map;

/**
 * @author Christian Schlichtherle
 */
//@ServiceImplementation
public class MyKeyManagerMapModifier implements KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class, new PromptingKeyManager<>(new MyPromptingKeyView()));
        return map;
    }
}
