/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.console;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.prompting.PromptingKeyManager;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;

import java.util.Map;

/**
 * A service provider for a console prompting key manager for {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -300)
public final class ConsoleAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        if (null != System.console()) {
            map.put(AesPbeParameters.class, new PromptingKeyManager<>(new ConsoleAesPbeParametersView()));
        }
        return map;
    }
}
