/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.key.api.KeyManager;
import global.namespace.truevfs.comp.key.api.aes.AesPbeParameters;
import global.namespace.truevfs.comp.key.api.prompting.PromptingKeyManager;
import global.namespace.truevfs.comp.key.api.spi.KeyManagerMapModifier;

import java.awt.*;
import java.util.Map;

/**
 * A service provider for a Swing prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -200)
public final class SwingAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        if (!GraphicsEnvironment.isHeadless()) {
            map.put(AesPbeParameters.class, new PromptingKeyManager<>(new SwingAesPbeParametersView()));
        }
        return map;
    }
}
