/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.impl;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;

/**
 * This modifier maps {@link AesPbeParameters} to a
 * {@linkplain DefaultKeyManager default key manager implementation}
 * which fails to resolve any secret keys.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
@Immutable
public final class DefaultAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class, new DefaultKeyManager());
        return map;
    }

    /** @return {@link Integer#MIN_VALUE} */
    @Override
    public int getPriority() { return Integer.MIN_VALUE; }
}
