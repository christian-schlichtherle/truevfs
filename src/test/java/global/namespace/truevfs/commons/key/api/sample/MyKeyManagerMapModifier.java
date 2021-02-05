/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api.sample;

import global.namespace.truevfs.commons.key.api.KeyManager;
import global.namespace.truevfs.commons.key.api.common.AesPbeParameters;
import global.namespace.truevfs.commons.key.api.prompting.PromptingKeyManager;
import global.namespace.truevfs.commons.key.api.spi.KeyManagerMapModifier;

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
