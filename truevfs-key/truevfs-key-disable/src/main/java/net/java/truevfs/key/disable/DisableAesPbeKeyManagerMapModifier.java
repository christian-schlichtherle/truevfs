/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.disable;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;
import net.java.truevfs.key.spec.unknown.UnknownKeyManager;

/**
 * This modifier maps the {@link AesPbeParameters} class to the
 * {@linkplain UnknownKeyManager#SINGLETON unknown key manager} singleton
 * which fails to resolve any secret keys.
 * Note that the {@link #getPriority} of this modifier is
 * {@link Integer#MAX_VALUE}, so it takes precedence over any other modifier
 * on the class path.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class DisableAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class, UnknownKeyManager.SINGLETON);
        return map;
    }

    /** @return {@link Integer#MAX_VALUE} */
    @Override
    public int getPriority() { return Integer.MAX_VALUE; }
}
