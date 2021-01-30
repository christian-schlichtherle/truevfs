/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx;

import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.spi.KeyManagerMapModifier;
import net.java.truecommons.shed.Option;

import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * Adds the {@link OsxKeyManager} to the map if and only if this JVM is running
 * on Mac OS X.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
@Immutable
@ServiceImplementation
public final class OsxAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {

    @Override
    @SuppressWarnings("unchecked")
    public Map<Class<?>, KeyManager<?>> apply(
            final Map<Class<?>, KeyManager<?>> map) {
        if ("Mac OS X".equals(System.getProperty("os.name"))) {
            for (final KeyManager<AesPbeParameters> km : Option.apply((KeyManager<AesPbeParameters>) map.get(AesPbeParameters.class))) {
                map.put(AesPbeParameters.class, new OsxKeyManager<>(km, AesPbeParameters.class));
                return map;
            }
            throw new ServiceConfigurationError(
                    "This module is a pure persistence service and depends on another key manager module to implement the user interface.");
        } else {
            return map;
        }
    }

    /** @return -100 */
    @Override
    public int getPriority() { return -100; }
}
