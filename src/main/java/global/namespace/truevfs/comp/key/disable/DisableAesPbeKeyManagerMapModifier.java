/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.disable;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.key.api.KeyManager;
import global.namespace.truevfs.comp.key.api.common.AesPbeParameters;
import global.namespace.truevfs.comp.key.api.spi.KeyManagerMapModifier;
import global.namespace.truevfs.comp.key.api.unknown.UnknownKeyManager;

import java.util.Map;

/**
 * This modifier maps the {@link AesPbeParameters} class to the
 * {@linkplain UnknownKeyManager#SINGLETON unknown key manager} singleton
 * which fails to resolve any keys.
 * Note that the {@link ServiceImplementation#priority()} of this modifier is {@link Integer#MAX_VALUE}, so it takes
 * precedence over any other modifier on the class path.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = Integer.MAX_VALUE)
public final class DisableAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class, UnknownKeyManager.SINGLETON);
        return map;
    }
}
