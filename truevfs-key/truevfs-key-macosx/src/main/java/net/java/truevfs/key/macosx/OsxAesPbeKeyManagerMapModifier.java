/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx;

import java.util.Map;
import java.util.ServiceConfigurationError;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;

/**
 *
 * @since  TrueVFS 0.10
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class OsxAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(
            final Map<Class<?>, KeyManager<?>> map) {
        if (!"Mac OS X".equals(System.getProperty("os.name"))) return map;
        final KeyManager<?> m = map.get(AesPbeParameters.class);
        if (null == m)
            throw new ServiceConfigurationError(
                "This module is a pure persistence service and depends on another key manager module to implement the user interface.");
        map.put(AesPbeParameters.class,
                new OsxKeyManager<>((KeyManager<AesPbeParameters>) m, AesPbeParameters.class));
        return map;
    }

    /** @return 100 */
    @Override
    public int getPriority() { return 100; }
}
