/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;

/**
 * Adds the {@link OsxKeyManager} to the map if and only if this JVM is running on Mac OS X.
 *
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation(priority = -100)
public final class OsxAesPbeKeyManagerMapModifier implements KeyManagerMapModifier {

    @Override
    @SuppressWarnings("unchecked")
    public Map<Class<?>, KeyManager<?>> apply(
            final Map<Class<?>, KeyManager<?>> map) {
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            final Optional<KeyManager<AesPbeParameters>> okm =
                    Optional.ofNullable((KeyManager<AesPbeParameters>) map.get(AesPbeParameters.class));
            if (okm.isPresent()) {
                final KeyManager<AesPbeParameters> km = okm.get();
                map.put(AesPbeParameters.class, new OsxKeyManager<>(km, AesPbeParameters.class));
                return map;
            }
            throw new ServiceConfigurationError(
                    "This module is a pure persistence service and depends on another key manager module to implement the user interface.");
        } else {
            return map;
        }
    }
}
