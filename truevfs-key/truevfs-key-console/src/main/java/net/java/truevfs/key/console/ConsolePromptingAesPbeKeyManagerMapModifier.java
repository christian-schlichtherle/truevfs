/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import net.java.truevfs.key.spec.spi.KeyManagerMapModifier;

/**
 * A service provider for a console prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class ConsolePromptingAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {
    @Override
    public Map<Class<?>, KeyManager<?>> apply(final Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class,
                new ConsolePromptingKeyManager<>(new ConsoleAesPbeParametersView()));
        return map;
    }

    /** @return -100 if console I/O is available, -300 otherwise. */
    @Override
    public int getPriority() {
        return null == System.console() ? -300 : -100;
    }
}
