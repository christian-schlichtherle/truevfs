/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macos;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.key.api.KeyManager;
import global.namespace.truevfs.comp.key.api.aes.AesPbeParameters;
import global.namespace.truevfs.comp.key.api.spi.KeyManagerMapModifier;

import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * Adds the {@link MacosKeyManager} to the map if and only if this JVM is running on Mac OS X.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class MacosAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @SuppressWarnings("unchecked")
    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            if (null == map.computeIfPresent(AesPbeParameters.class, (keyClass, manager) -> MacosKeyManager.create((KeyManager<AesPbeParameters>) manager, (Class<AesPbeParameters>) keyClass))) {
                throw new ServiceConfigurationError(
                        "This module is a pure persistence service and depends on another key manager module to implement the user interface.");
            }
        }
        return map;
    }
}
