/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;

import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * Adds the {@link OsxKeyManager} to the map if and only if this JVM is running on Mac OS X.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class OsxAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @SuppressWarnings("unchecked")
    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            if (null == map.computeIfPresent(AesPbeParameters.class, (klass, keyManager) -> new OsxKeyManager(keyManager, klass))) {
                throw new ServiceConfigurationError(
                        "This module is a pure persistence service and depends on another key manager module to implement the user interface.");
            }
        }
        return map;
    }
}
