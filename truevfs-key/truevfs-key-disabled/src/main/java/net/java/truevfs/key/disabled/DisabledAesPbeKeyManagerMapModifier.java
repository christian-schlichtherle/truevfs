/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.disabled;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;

/**
 * This modifier maps {@link AesPbeParameters} to a key manager which fails to
 * resolve any passwords.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
@Immutable
public final class DisabledAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class, new DisabledKeyManager());
        return map;
    }

    /** @return {@link Integer#MAX_VALUE} */
    @Override
    public int getPriority() { return Integer.MAX_VALUE; }
}
