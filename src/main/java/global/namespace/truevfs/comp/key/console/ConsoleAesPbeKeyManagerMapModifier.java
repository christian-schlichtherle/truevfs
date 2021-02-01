/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.console;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.key.spec.KeyManager;
import global.namespace.truevfs.comp.key.spec.common.AesPbeParameters;
import global.namespace.truevfs.comp.key.spec.prompting.PromptingKeyManager;
import global.namespace.truevfs.comp.key.spec.spi.KeyManagerMapModifier;

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
