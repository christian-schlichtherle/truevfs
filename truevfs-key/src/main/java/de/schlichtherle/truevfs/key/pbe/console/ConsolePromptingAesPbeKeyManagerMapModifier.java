/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.console;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.key.KeyManager;
import net.truevfs.key.param.AesPbeParameters;
import net.truevfs.key.spi.KeyManagerMapModifier;

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

    /** @return -50 if console I/O is available, -150 otherwise. */
    @Override
    public int getPriority() {
        return null == System.console() ? -150 : -50;
    }
}
