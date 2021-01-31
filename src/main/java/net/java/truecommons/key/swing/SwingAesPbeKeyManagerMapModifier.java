/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.prompting.PromptingKeyManager;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;

import javax.annotation.concurrent.Immutable;
import java.awt.*;
import java.util.Map;

/**
 * A service provider for a Swing prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation(priority = -200)
public final class SwingAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        if (!GraphicsEnvironment.isHeadless())
            map.put(AesPbeParameters.class,
                    new PromptingKeyManager<>(new SwingAesPbeParametersView()));
        return map;
    }
}
